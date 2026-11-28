package com.kapm.onboarding_clientes.service;

import com.kapm.onboarding_clientes.dto.response.CodigoPostalResponse;
import com.kapm.onboarding_clientes.exception.ErrorMessage;
import com.kapm.onboarding_clientes.exception.RecursoNoEncontradoException;
import com.kapm.onboarding_clientes.exception.ReglaNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * Autocompletado del domicilio por código postal con el Catálogo Nacional de Códigos Postales de Correos de México.
 * <p>
 * El catálogo es gratuito pero no se puede redistribuir, así que no viaja en el repositorio: se descarga del sitio
 * oficial la primera vez, se guarda en una caché local (se renueva cada {@code dias-cache} días) y se indexa en memoria.
 */
@Slf4j
@Service
public class CodigoPostalService {

    /** Clave INEGI del estado (c_estado) → código del catálogo de estados. */
    private static final Map<String, String> ESTADOS_INEGI = Map.ofEntries(
            Map.entry("01", "AGUASCALIENTES"), Map.entry("02", "BAJA_CALIFORNIA"), Map.entry("03", "BAJA_CALIFORNIA_SUR"),
            Map.entry("04", "CAMPECHE"), Map.entry("05", "COAHUILA"), Map.entry("06", "COLIMA"), Map.entry("07", "CHIAPAS"),
            Map.entry("08", "CHIHUAHUA"), Map.entry("09", "CIUDAD_DE_MEXICO"), Map.entry("10", "DURANGO"),
            Map.entry("11", "GUANAJUATO"), Map.entry("12", "GUERRERO"), Map.entry("13", "HIDALGO"), Map.entry("14", "JALISCO"),
            Map.entry("15", "ESTADO_DE_MEXICO"), Map.entry("16", "MICHOACAN"), Map.entry("17", "MORELOS"),
            Map.entry("18", "NAYARIT"), Map.entry("19", "NUEVO_LEON"), Map.entry("20", "OAXACA"), Map.entry("21", "PUEBLA"),
            Map.entry("22", "QUERETARO"), Map.entry("23", "QUINTANA_ROO"), Map.entry("24", "SAN_LUIS_POTOSI"),
            Map.entry("25", "SINALOA"), Map.entry("26", "SONORA"), Map.entry("27", "TABASCO"), Map.entry("28", "TAMAULIPAS"),
            Map.entry("29", "TLAXCALA"), Map.entry("30", "VERACRUZ"), Map.entry("31", "YUCATAN"), Map.entry("32", "ZACATECAS"));

    private static final String PAIS = "MEXICO";

    record Datos(String estado, String municipio, List<String> colonias) {
    }

    private final String url;
    private final Path cache;
    private final int diasCache;
    private final boolean precarga;
    private volatile Map<String, Datos> indice;
    private volatile Map<String, List<String>> municipiosPorEstado;
    private volatile Map<String, List<String>> coloniasPorMunicipio;

    public CodigoPostalService(
            @Value("${app.codigos-postales.url:https://www.correosdemexico.gob.mx/datosabiertos/cp/cpdescarga.txt}") String url,
            @Value("${app.codigos-postales.cache:${java.io.tmpdir}/onboarding-clientes/cpdescarga.txt}") String cache,
            @Value("${app.codigos-postales.dias-cache:30}") int diasCache,
            @Value("${app.codigos-postales.precarga:true}") boolean precarga) {
        this.url = url;
        this.cache = Path.of(cache);
        this.diasCache = diasCache;
        this.precarga = precarga;
    }

    /** Carga el catálogo en segundo plano al arrancar para que la primera consulta sea inmediata. */
    @EventListener(ApplicationReadyEvent.class)
    public void precargar() {
        if (precarga) {
            CompletableFuture.runAsync(() -> {
                try {
                    indice();
                } catch (Exception e) {
                    log.warn("No se pudo precargar el catálogo de códigos postales: {}", e.getMessage());
                }
            });
        }
    }

    public CodigoPostalResponse consultar(String codigoPostal) {
        String cp = codigoPostal == null ? "" : codigoPostal.trim();
        if (!cp.matches("^\\d{5}$")) {
            throw new ReglaNegocioException(ErrorMessage.CODIGO_POSTAL_INVALIDO);
        }
        Datos datos = indice().get(cp);
        if (datos == null) {
            throw new RecursoNoEncontradoException(ErrorMessage.CODIGO_POSTAL_NO_ENCONTRADO);
        }
        List<String> colonias = combinarColonias(datos.colonias(), coloniasPorMunicipio(datos.estado(), datos.municipio()));
        return CodigoPostalResponse.builder()
                .codigoPostal(cp)
                .estado(datos.estado())
                .municipio(datos.municipio())
                .pais(PAIS)
                .colonias(colonias)
                .build();
    }

    /** Municipios o alcaldías distintos y ordenados alfabéticamente para un código de estado del catálogo. */
    public List<String> municipiosPorEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return List.of();
        }
        String clave = estado.trim().toUpperCase(Locale.ROOT);
        indice();
        Map<String, List<String>> mapa = municipiosPorEstado;
        return mapa != null ? mapa.getOrDefault(clave, List.of()) : List.of();
    }

    /** Colonias o asentamientos distintos y ordenados alfabéticamente de un municipio dentro de un estado. */
    public List<String> coloniasPorMunicipio(String estado, String municipio) {
        if (estado == null || estado.isBlank() || municipio == null || municipio.isBlank()) {
            return List.of();
        }
        indice();
        Map<String, List<String>> mapa = coloniasPorMunicipio;
        if (mapa == null) {
            return List.of();
        }
        return mapa.getOrDefault(claveMunicipio(estado, municipio), List.of());
    }

    private Map<String, Datos> indice() {
        Map<String, Datos> actual = indice;
        if (actual != null) {
            return actual;
        }
        synchronized (this) {
            if (indice == null) {
                try {
                    descargarSiHaceFalta();
                    Map<String, Datos> cargado;
                    try (Reader reader = Files.newBufferedReader(cache, StandardCharsets.ISO_8859_1)) {
                        cargado = parsear(reader);
                    }
                    municipiosPorEstado = agruparMunicipios(cargado);
                    coloniasPorMunicipio = agruparColoniasPorMunicipio(cargado);
                    indice = cargado;
                    log.info("Catálogo de códigos postales cargado: {} códigos", indice.size());
                } catch (Exception e) {
                    log.warn("Catálogo de códigos postales no disponible: {}", e.getMessage());
                    throw new ReglaNegocioException(ErrorMessage.CODIGOS_POSTALES_NO_DISPONIBLES);
                }
            }
            return indice;
        }
    }

    private static Map<String, List<String>> agruparColoniasPorMunicipio(Map<String, Datos> datosPorCp) {
        Collator collator = Collator.getInstance(Locale.of("es", "MX"));
        collator.setStrength(Collator.PRIMARY);
        Map<String, TreeSet<String>> porMunicipio = new HashMap<>();
        for (Datos d : datosPorCp.values()) {
            if (d.municipio() != null && !d.municipio().isBlank() && d.colonias() != null) {
                TreeSet<String> set = porMunicipio.computeIfAbsent(
                        claveMunicipio(d.estado(), d.municipio()), k -> new TreeSet<>(collator));
                set.addAll(d.colonias());
            }
        }
        Map<String, List<String>> resultado = new HashMap<>(porMunicipio.size());
        porMunicipio.forEach((clave, set) -> resultado.put(clave, List.copyOf(set)));
        return resultado;
    }

    /** Mantiene primero las colonias exactas del CP y añade el resto del municipio sin duplicados. */
    private static List<String> combinarColonias(List<String> delCp, List<String> delMunicipio) {
        if (delMunicipio == null || delMunicipio.isEmpty()) {
            return delCp;
        }
        Map<String, String> unicas = new LinkedHashMap<>();
        for (String c : delCp) {
            unicas.putIfAbsent(normalizarClave(c), c);
        }
        for (String c : delMunicipio) {
            unicas.putIfAbsent(normalizarClave(c), c);
        }
        return List.copyOf(unicas.values());
    }

    private static String claveMunicipio(String estado, String municipio) {
        return estado.trim().toUpperCase(Locale.ROOT) + "|" + normalizarClave(municipio);
    }

    private static String normalizarClave(String texto) {
        String sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return sinAcentos.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static Map<String, List<String>> agruparMunicipios(Map<String, Datos> datosPorCp) {
        Collator collator = Collator.getInstance(Locale.of("es", "MX"));
        collator.setStrength(Collator.PRIMARY);
        Map<String, TreeSet<String>> porEstado = new HashMap<>();
        for (Datos d : datosPorCp.values()) {
            if (d.municipio() != null && !d.municipio().isBlank()) {
                porEstado.computeIfAbsent(d.estado(), k -> new TreeSet<>(collator)).add(d.municipio());
            }
        }
        Map<String, List<String>> resultado = new HashMap<>(porEstado.size());
        porEstado.forEach((estado, set) -> resultado.put(estado, List.copyOf(set)));
        return resultado;
    }

    private void descargarSiHaceFalta() throws IOException, InterruptedException {
        boolean vigente = Files.exists(cache) && Files.size(cache) > 0
                && Files.getLastModifiedTime(cache).toInstant().isAfter(Instant.now().minus(Duration.ofDays(diasCache)));
        if (vigente) {
            return;
        }
        log.info("Descargando el Catálogo Nacional de Códigos Postales de Correos de México");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpResponse<InputStream> respuesta = client.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120)).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
        if (respuesta.statusCode() != 200) {
            throw new IOException("HTTP " + respuesta.statusCode() + " al descargar el catálogo de códigos postales");
        }
        Files.createDirectories(cache.toAbsolutePath().getParent());
        Path temporal = Files.createTempFile(cache.toAbsolutePath().getParent(), "cp", ".tmp");
        try (InputStream in = respuesta.body()) {
            Files.copy(in, temporal, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(temporal, cache, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Formato oficial (separado por |, ISO-8859-1): primera línea de aviso, segunda de encabezados y luego
     * d_codigo|d_asenta|d_tipo_asenta|D_mnpio|d_estado|d_ciudad|d_CP|c_estado|...
     */
    static Map<String, Datos> parsear(Reader reader) throws IOException {
        Map<String, Datos> resultado = new HashMap<>(40_000);
        BufferedReader lector = new BufferedReader(reader);
        String linea;
        while ((linea = lector.readLine()) != null) {
            String[] c = linea.split("\\|", -1);
            if (c.length < 8 || !c[0].matches("^\\d{5}$")) {
                continue; // aviso, encabezados o líneas incompletas
            }
            String estado = ESTADOS_INEGI.get(c[7].trim());
            if (estado == null) {
                continue;
            }
            Datos datos = resultado.computeIfAbsent(c[0], k -> new Datos(estado, c[3].trim(), new ArrayList<>()));
            String colonia = c[1].trim();
            if (!colonia.isEmpty() && !datos.colonias().contains(colonia)) {
                datos.colonias().add(colonia);
            }
        }
        return resultado;
    }
}
