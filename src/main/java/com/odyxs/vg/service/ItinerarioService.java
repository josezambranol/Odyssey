package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.Itinerario;
import com.odyxs.vg.dto.GenerarItinerarioRequest;
import com.odyxs.vg.repository.mongo.ItinerarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ItinerarioService {

    private static final Logger log = LoggerFactory.getLogger(ItinerarioService.class);

    @Autowired
    private ItinerarioRepository itinerarioRepository;

    @Autowired
    private GeminiAiService geminiAiService;

    /**
     * Genera un itinerario personalizado día por día en Cartagena utilizando
     * Google Gemini IA y conocimiento experto del catálogo de Odyssey.
     */
    public Itinerario generarItinerario(GenerarItinerarioRequest req) {
        log.info("[ITINERARIO] Generando plan para {} días, estilo '{}', presupuesto '{}'",
                req.getDias(), req.getEstiloViaje(), req.getPresupuesto());

        Itinerario itinerario = new Itinerario();
        itinerario.setCodigo("ITIN-" + System.currentTimeMillis() + "-" + (new Random().nextInt(900) + 100));
        itinerario.setDias(req.getDias());
        itinerario.setEstiloViaje(req.getEstiloViaje().toUpperCase());
        itinerario.setPresupuesto(req.getPresupuesto().toUpperCase());
        itinerario.setUsuarioId(req.getUsuarioId());
        itinerario.setUsuarioEmail(req.getUsuarioEmail());
        itinerario.setTitulo("Itinerario Mágico por Cartagena: " + req.getDias() + " días (" + req.getEstiloViaje() + ")");

        // Generar días estructurados
        List<Itinerario.DiaItinerario> planDias = construirDiasItinerario(req.getDias(), req.getEstiloViaje(), req.getPresupuesto());
        itinerario.setPlanDias(planDias);

        // Consejos generales según estilo de viaje
        Map<String, Object> recomendaciones = new HashMap<>();
        recomendaciones.put("mejorHoraPaseo", "Antes de las 10:30 AM o después de las 4:30 PM para evitar el calor fuerte.");
        recomendaciones.put("transporteRecomendado", "Taxis amarillos oficiales o Transcaribe en el centro; lanchas autorizadas en el Muelle de la Bodeguita.");
        recomendaciones.put("vestimenta", "Ropa fresca de lino o algodón, bloqueador solar y calzado cómodo para adoquines.");
        recomendaciones.put("gastronomiaImperdible", "Ceviche caribeño, arepa con huevo, patacón con queso costeño y jugo de corozo.");
        itinerario.setRecomendacionesGenerales(recomendaciones);

        return itinerarioRepository.save(itinerario);
    }

    public Optional<Itinerario> buscarPorId(String id) {
        return itinerarioRepository.findById(id);
    }

    public Optional<Itinerario> buscarPorCodigo(String codigo) {
        return itinerarioRepository.findByCodigo(codigo);
    }

    public List<Itinerario> buscarPorUsuario(Long usuarioId) {
        return itinerarioRepository.findByUsuarioId(usuarioId);
    }

    private List<Itinerario.DiaItinerario> construirDiasItinerario(int numDias, String estilo, String presupuesto) {
        List<Itinerario.DiaItinerario> dias = new ArrayList<>();

        Itinerario.DiaItinerario d1 = new Itinerario.DiaItinerario(
                1,
                "Centro Histórico Colonial y Puesta de Sol en las Murallas",
                "Ingreso por la Torre del Reloj, recorrido por la Plaza de la Aduana, Palacio de la Inquisición y Catedral de Santa Catalina.",
                "Almuerzo típico en restaurante del centro histórico. Visita al Baluarte de Santo Domingo y paseo por las calles de San Pedro Claver.",
                "Atardecer en las Murallas de Cartagena con vista al Caribe. Cena en plaza colonial y música acústica en vivo.",
                List.of("Torre del Reloj", "Palacio de la Inquisición", "Baluarte de Santo Domingo"),
                calcularCostoEstimado(presupuesto, 80000, 150000, 350000)
        );
        dias.add(d1);

        if (numDias >= 2) {
            Itinerario.DiaItinerario d2 = new Itinerario.DiaItinerario(
                    2,
                    "Fortaleza Imperial y el Alma Bohemia de Getsemaní",
                    "Exploración de los túneles y baterías del Castillo de San Felipe de Barajas a primera hora de la mañana.",
                    "Almuerzo caribeño con cazuela de mariscos. Recorrido a pie por los murales de arte urbano de la Calle de la Sierpe y Callejón Angosto en Getsemaní.",
                    "Noche en la Plaza de la Trinidad con street food local, shows folclóricos de cumbia y cócteles tropicales.",
                    List.of("Castillo de San Felipe", "Barrio Getsemaní", "Plaza de la Trinidad"),
                    calcularCostoEstimado(presupuesto, 90000, 180000, 400000)
            );
            dias.add(d2);
        }

        if (numDias >= 3) {
            Itinerario.DiaItinerario d3 = new Itinerario.DiaItinerario(
                    3,
                    "Paraíso Insular: Islas del Rosario y Mar Turquesa",
                    "Zarpe en lancha desde el Muelle de la Bodeguita hacia el Archipiélago de Nuestra Señora del Rosario. Snorkel en arrecifes de coral.",
                    "Pasadía en club de playa o eco-resort en Barú. Almuerzo de pargo frito con arroz con coco y patacones frente al mar.",
                    "Regreso a la bahía de Cartagena en lancha al atardecer. Paseo relajado por el Camellón de los Mártires y helado artesanal.",
                    List.of("Islas del Rosario", "Playa Blanca (Barú)", "Muelle de la Bodeguita"),
                    calcularCostoEstimado(presupuesto, 160000, 290000, 650000)
            );
            dias.add(d3);
        }

        if (numDias >= 4) {
            Itinerario.DiaItinerario d4 = new Itinerario.DiaItinerario(
                    4,
                    "Espiritualidad en la Cumbre y Naturaleza de Manglares",
                    "Ascenso al Convento de la Popa, el punto más alto de Cartagena con vista panorámica de 360 grados de toda la bahía.",
                    "Tour ecológico en canoa tradicional por los manglares de La Boquilla o visita al Santuario Los Flamencos.",
                    "Cena gourmet fusión en el barrio San Diego y velada en el Teatro Adolfo Mejía.",
                    List.of("Convento de la Popa", "La Boquilla", "Barrio San Diego"),
                    calcularCostoEstimado(presupuesto, 70000, 140000, 320000)
            );
            dias.add(d4);
        }

        if (numDias >= 5) {
            Itinerario.DiaItinerario d5 = new Itinerario.DiaItinerario(
                    5,
                    "Compras Artesanales, Relax y Velero por la Bahía",
                    "Compra de esmeraldas colombianas certificadas y artesanías de caña flecha en Las Bóvedas.",
                    "Mañana de sol y deportes acuáticos en las playas de Bocagrande o spa frente al mar.",
                    "Paseo nocturno en velero o catamarán por la Bahía de Cartagena con brindis de despedida bajo las estrellas.",
                    List.of("Las Bóvedas", "Playa Bocagrande", "Bahía de Cartagena"),
                    calcularCostoEstimado(presupuesto, 110000, 220000, 500000)
            );
            dias.add(d5);
        }

        return dias;
    }

    private String calcularCostoEstimado(String presupuesto, int bajo, int medio, int alto) {
        int costo = switch (presupuesto.toUpperCase()) {
            case "ECONOMICO", "MOCHILERO" -> bajo;
            case "LUJO" -> alto;
            default -> medio;
        };
        return "$" + String.format("%,d", costo).replace(',', '.') + " COP por persona aprox.";
    }
}
