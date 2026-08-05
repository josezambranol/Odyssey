CREATE DATABASE  IF NOT EXISTS `odyxsvg_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */;
USE `odyxsvg_db`;
-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: odyxsvg_db
-- ------------------------------------------------------
-- Server version	5.5.5-10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `actividades`
--

DROP TABLE IF EXISTS `actividades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `actividades` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `descripcion` varchar(1000) DEFAULT NULL,
  `duracion` varchar(255) DEFAULT NULL,
  `precio_aprox` varchar(255) DEFAULT NULL,
  `imagen_url` varchar(500) DEFAULT NULL,
  `categoria` enum('TOUR','DEPORTE','CULTURA','GASTRONOMIA','NATURALEZA') NOT NULL DEFAULT 'TOUR',
  `es_oficial` bit(1) NOT NULL,
  `estado` enum('APROBADO','PENDIENTE','RECHAZADO') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `actividades`
--

LOCK TABLES `actividades` WRITE;
/*!40000 ALTER TABLE `actividades` DISABLE KEYS */;
INSERT INTO `actividades` VALUES (1,'Tour al Castillo de San Felipe','Recorrido guiado por los túneles y bastiones del castillo más imponente de América.','2 horas','$25.000 COP','/uploads/actividades/1779056957701_act-castillo-sanfelipe.jpeg','CULTURA',_binary '\0','APROBADO'),(2,'Buceo en Islas del Rosario','Bautismo de buceo o inmersiones en los arrecifes coralinos más vírgenes del Caribe.','Medio día','Desde $180.000 COP','/uploads/actividades/1779057155948_Buceo-en-Cartagena-con-Exploria-5.jpg','DEPORTE',_binary '\0','APROBADO'),(3,'Tour en Chiva Rumbera','La discoteca rodante más famosa del Caribe. Música vallenato y costeña por las calles de Cartagena.','3 horas','$50.000 COP','/uploads/actividades/1779077229258_chiva-rumbera.jpg','TOUR',_binary '\0','APROBADO'),(4,'Clase de Cocina Caribeña','Aprende a preparar ceviche, arepas de chócolo y cazuela de mariscos con chefs locales.','3 horas','$120.000 COP','/uploads/actividades/1779057219818_act-gastronomia.jpg','GASTRONOMIA',_binary '\0','APROBADO'),(6,'Tour Getsemaní a pie','Barrio artístico lleno de murales callejeros, historia y gastronomía. El barrio más auténtico de Cartagena.','2 horas','$35.000 COP','/uploads/actividades/1779057471641_getsemani-cartagena.jpg','TOUR',_binary '\0','APROBADO'),(7,'Avistamiento de Flamencos','Visita al Santuario Los Flamencos. Cientos de flamencos rosados en su hábitat natural.','Día completo','$150.000 COP','/uploads/actividades/1779057561329_avistamento de flamencos.jpg','NATURALEZA',_binary '\0','APROBADO'),(8,'Pesca Deportiva','Pesca de altura en el Mar Caribe a bordo de embarcaciones especializadas.','4 horas','$250.000 COP','/uploads/actividades/1779057732991_pesca-deportiva.jpg','DEPORTE',_binary '\0','APROBADO');
/*!40000 ALTER TABLE `actividades` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categorias`
--

DROP TABLE IF EXISTS `categorias`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categorias` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `icono` varchar(10) DEFAULT '?',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cat_nombre` (`nombre`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categorias`
--

LOCK TABLES `categorias` WRITE;
/*!40000 ALTER TABLE `categorias` DISABLE KEYS */;
INSERT INTO `categorias` VALUES (1,'PLAYAS','?️'),(2,'MUSEOS','?️'),(3,'CINES','?'),(4,'HOTELES','?'),(5,'SITIOS HISTÓRICOS','?'),(6,'FESTIVALES','?'),(7,'GASTRONOMÍA','?️');
/*!40000 ALTER TABLE `categorias` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `chatbot_respuestas`
--

DROP TABLE IF EXISTS `chatbot_respuestas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `chatbot_respuestas` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `clave` varchar(100) NOT NULL,
  `respuesta` varchar(2000) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clave` (`clave`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `chatbot_respuestas`
--

LOCK TABLES `chatbot_respuestas` WRITE;
/*!40000 ALTER TABLE `chatbot_respuestas` DISABLE KEYS */;
INSERT INTO `chatbot_respuestas` VALUES (1,'saludo','¡Hola! ? Soy ODYX, tu guía inteligente de Cartagena de Indias. Puedo ayudarte con información sobre lugares turísticos, clima, transporte, eventos y actividades. ¿Qué quieres descubrir hoy?'),(2,'clima','?️ Cartagena tiene un clima tropical cálido durante todo el año.\n\n?️ Temperatura promedio: 28–32°C\n?️ Temporada lluviosa: mayo–noviembre\n☀️ Temporada seca (ideal): diciembre–abril\n\nEl mejor momento para visitar es entre diciembre y marzo, cuando el clima es seco y soleado.'),(3,'playas','?️ Las mejores playas de Cartagena:\n\n1. Playa Blanca (Isla de Barú) — la más famosa, aguas turquesa ✨\n2. Bocagrande — la más accesible desde el centro\n3. Islas del Rosario — para snorkel y buceo\n4. La Boquilla — cocina costeña y manglares\n5. Múcura — la más virgen del archipiélago\n\n¿Quieres saber cómo llegar a alguna de ellas?'),(4,'transporte','? Cómo moverse en Cartagena:\n\n• Transcaribe: bus articulado, $3.900 COP\n• Taxi convencional: $13.000–$40.000 COP\n• InDriver/Uber: desde $12.000 COP\n• Yate a Islas del Rosario: $60.000–$150.000 COP\n• Coches eléctricos: tour colonial de noche\n\nPara el centro histórico, lo mejor es ir a pie o en bicitaxi.'),(5,'hoteles','? Hoteles recomendados en Cartagena:\n\n⭐⭐⭐⭐⭐ Lujo:\n• Charleston Santa Teresa (Centro Histórico)\n• Sofitel Legend Santa Clara\n• Bastión Luxury Hotel\n\n⭐⭐⭐⭐ Confort:\n• Hotel Almirante Cartagena (Bocagrande)\n• GHL Hotel Capilla del Mar\n\n? Económico:\n• Hostal Casa Vieja (Getsemaní)\n\nTodos los hoteles están disponibles en la sección \"Dónde Alojarse\" de ODYXS.'),(6,'historico','? Historia de Cartagena:\n\nFundada en 1533 por Pedro de Heredia, Cartagena fue el principal puerto colonial de España en América del Sur. Sus imponentes murallas y fortines la hicieron inexpugnable.\n\nEn 1811 proclamó su independencia, siendo la primera ciudad en Colombia. En 1984 fue declarada Patrimonio de la Humanidad por la UNESCO.\n\n?️ Lugares clave:\n• Castillo de San Felipe (1536)\n• Murallas coloniales (11 km)\n• Torre del Reloj\n• Plaza de Bolívar'),(7,'actividades','? Qué hacer en Cartagena:\n\n?️ Cultura: Tour al Castillo, Museos, Getsemaní\n? Aventura: Buceo, Snorkel, Kayak en manglares\n? Fiesta: Chiva Rumbera, Bares en Getsemaní\n?️ Gastronomía: Cevichería, Cocina caribeña\n⛵ Náutica: Islas del Rosario, Pesca deportiva\n? Naturaleza: Flamencos, Santuario fauna\n\nExplora todas en la sección \"Actividades\" de ODYXS.'),(8,'eventos','? Eventos principales en Cartagena:\n\n• Enero: Hay Festival (literatura y arte)\n• Marzo: FICCI (cine internacional)\n• Febrero: Regata Veleros del Caribe\n• Mayo: Noche de Museos\n• Noviembre: Independencia + Concurso Nacional de Belleza\n\nConsulta las fechas exactas en la sección \"Eventos\" de la plataforma.'),(9,'costo','? ¿Cuánto cuesta visitar Cartagena?\n\n? Temporada alta (dic–ene): precios altos\n? Temporada media (mar–jun): precios intermedios\n? Temporada baja (ago–oct): mejores precios\n\n? Presupuesto diario estimado:\n• Económico: $80.000–$150.000 COP\n• Confort: $250.000–$500.000 COP\n• Lujo: más de $800.000 COP\n\n(No incluye alojamiento)'),(10,'gastronomia','?️ Gastronomía de Cartagena:\n\n? Platos típicos:\n• Ceviche de camarones\n• Cazuela de mariscos\n• Arepas de chócolo con queso\n• Arroz con coco\n• Patacones con hogao\n\n? Bebidas:\n• Corozo (jugo de fruto tropical)\n• Ron caribeño\n• Agua de panela con limón\n\n? Restaurantes recomendados: La Cevichería, El Santísimo, Alma Restaurant (Hotel Santa Clara).'),(11,'despedida','¡Hasta pronto! ? Espero haberte ayudado a planear tu viaje a Cartagena. Recuerda explorar todos los lugares en ODYXS. ¡Que tengas una increíble estadía en la ciudad amurallada! ??');
/*!40000 ALTER TABLE `chatbot_respuestas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eventos`
--

DROP TABLE IF EXISTS `eventos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eventos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `descripcion` varchar(1000) DEFAULT NULL,
  `fecha` date NOT NULL,
  `lugar` varchar(255) DEFAULT NULL,
  `imagen_url` varchar(500) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eventos`
--

LOCK TABLES `eventos` WRITE;
/*!40000 ALTER TABLE `eventos` DISABLE KEYS */;
INSERT INTO `eventos` VALUES (1,'Hay Festival Cartagena 2026','El festival literario y de ideas más importante de Latinoamérica. Autores, pensadores y artistas de todo el mundo.','2026-01-28','Centro de Convenciones de Cartagena','/uploads/eventos/1779059419696.jpg',1),(2,'Festival Internacional de Cine de Cartagena (FICCI)','El festival de cine más antiguo de América Latina. Proyecciones en la ciudad amurallada.','2026-03-06','Centro Histórico, Cartagena','/uploads/eventos/1779059468552.jpg',1),(3,'Fiestas de la Independencia de Cartagena','Celebración del 11 de Noviembre con desfiles, música vallenato y festividades en toda la ciudad.','2026-11-11','Ciudad completa, Cartagena','/uploads/eventos/1779059484065.jpg',1),(4,'Concurso Nacional de Belleza','El evento de moda y belleza más importante de Colombia. Coronación en el Centro Histórico.','2026-11-11','Centro Histórico, Cartagena','/uploads/eventos/1779059580668.jpg',1),(5,'Regata Veleros del Caribe','Competencia náutica internacional en las aguas turquesa frente a Cartagena.','2026-02-14','Bahía de Cartagena','/uploads/eventos/1779059443486.jpg',1);
/*!40000 ALTER TABLE `eventos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lugares`
--

DROP TABLE IF EXISTS `lugares`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lugares` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `descripcion` varchar(1000) DEFAULT NULL,
  `ubicacion` varchar(255) NOT NULL,
  `url_mapa` varchar(1000) DEFAULT NULL,
  `imagen_url` varchar(500) DEFAULT NULL,
  `categoria_id` bigint(20) DEFAULT NULL,
  `es_oficial` bit(1) NOT NULL DEFAULT b'0',
  `estado` enum('APROBADO','PENDIENTE','RECHAZADO') NOT NULL DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id`),
  KEY `fk_lugar_cat` (`categoria_id`),
  CONSTRAINT `fk_lugar_cat` FOREIGN KEY (`categoria_id`) REFERENCES `categorias` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lugares`
--

LOCK TABLES `lugares` WRITE;
/*!40000 ALTER TABLE `lugares` DISABLE KEYS */;
INSERT INTO `lugares` VALUES (1,'Playa Blanca – Isla de Barú','Famosa playa de arenas blancas y aguas turquesa. Ideal para snorkel y gastronomía caribeña.','Isla de Barú, 45 min desde Cartagena','https://maps.app.goo.gl/aftF3AwcDmpAoxKg9','/uploads/lugares/1779055872781.jpg',1,_binary '','APROBADO'),(2,'Playa de Bocagrande','La playa urbana más popular de Cartagena. Restaurantes y ambiente caribeño a pasos del centro.','Bocagrande, Cartagena','https://maps.app.goo.gl/bocagrande','/uploads/lugares/1779055543752.jpg',1,_binary '','APROBADO'),(3,'Museo del Oro Zenú','Más de 500 piezas de orfebrería precolombina de la cultura Zenú.','Plaza de Bolívar, Centro Histórico','https://maps.app.goo.gl/museooro','/uploads/lugares/1779055813929.jpg',2,_binary '','APROBADO'),(4,'Museo Naval del Caribe','Historia marítima y militar del Caribe colombiano. Siglo XVII.','Calle San Juan de Dios, Centro Histórico','https://maps.app.goo.gl/museonaval','/uploads/lugares/1779055906479.jpg',2,_binary '','APROBADO'),(5,'Cine Colombia Bocagrande','5 salas 2D y 3D en el CC Plaza Bocagrande.','CC Plaza Bocagrande Piso 4, Cartagena','https://maps.app.goo.gl/cinebocagrande','/uploads/lugares/1779056148735.jpg',3,_binary '','APROBADO'),(6,'Hotel Almirante Cartagena','Hotel de lujo frente al mar en Bocagrande. Piscina y acceso directo a la playa.','Av. San Martín Calle 6, Bocagrande','https://hotelalmirantecartagena.com/','/uploads/lugares/1779056197982.jpg',4,_binary '\0','APROBADO'),(7,'Charleston Santa Teresa','Hotel boutique en convento restaurado del siglo XVII. Spa y piscina en azotea.','Carrera 3 #31-23, Plaza de Santa Teresa, Centro Histórico','https://www.hotelcharlestonsantateresa.com/','/uploads/lugares/1779056218413.jpg',4,_binary '','APROBADO'),(8,'Murallas de Cartagena','Sistema defensivo colonial declarado Patrimonio UNESCO. 11 km del siglo XVI.','Centro Histórico, Cartagena','https://maps.app.goo.gl/murallas','/uploads/eventos/1779050739373.jpg',5,_binary '','APROBADO'),(9,'Torre del Reloj','Símbolo icónico del siglo XVII. Entrada principal a la ciudad amurallada.','Plaza de los Coches, Centro Histórico','https://maps.app.goo.gl/torrereloj','/uploads/lugares/1779056249263.jpg',5,_binary '','APROBADO'),(10,'Castillo de San Felipe de Barajas','La fortaleza española más grande de América. Data de 1536. Patrimonio UNESCO.','Barrio San Lázaro, Cartagena','https://maps.app.goo.gl/sanfelipe','/uploads/lugares/1779056311119.jpg',5,_binary '','APROBADO'),(11,'La Cevichería','Especializado en mariscos y ceviche caribeño. Recomendado por Anthony Bourdain.','Calle Stuart #7-14, Getsemaní','https://maps.app.goo.gl/lacevicheria','/uploads/lugares/1779056390178.jpg',7,_binary '\0','APROBADO');
/*!40000 ALTER TABLE `lugares` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resenas`
--

DROP TABLE IF EXISTS `resenas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resenas` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `comentario` varchar(500) DEFAULT NULL,
  `calificacion` int(11) NOT NULL DEFAULT 5,
  `fecha` date NOT NULL DEFAULT curdate(),
  `lugar_id` bigint(20) DEFAULT NULL,
  `usuario_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_res_lugar` (`lugar_id`),
  KEY `fk_res_usuario` (`usuario_id`),
  CONSTRAINT `fk_res_lugar` FOREIGN KEY (`lugar_id`) REFERENCES `lugares` (`id`),
  CONSTRAINT `fk_res_usuario` FOREIGN KEY (`usuario_id`) REFERENCES `usuarios` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resenas`
--

LOCK TABLES `resenas` WRITE;
/*!40000 ALTER TABLE `resenas` DISABLE KEYS */;
INSERT INTO `resenas` VALUES (1,'¡Playa Blanca es increíble! Las aguas turquesa son un paraíso.',5,'2026-04-29',1,2),(2,'El Castillo de San Felipe es imponente. La historia que cuenta es fascinante.',5,'2026-05-01',10,2),(3,'Las murallas al atardecer son espectaculares. Vista al Caribe.',5,'2026-05-04',8,2),(4,'Hotel demasiado bueno recomendado 100%\r\n',3,'2026-05-18',7,5);
/*!40000 ALTER TABLE `resenas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuarios`
--

DROP TABLE IF EXISTS `usuarios`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuarios` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(255) NOT NULL,
  `correo` varchar(255) NOT NULL,
  `contrasena` varchar(255) NOT NULL,
  `fecha_nacimiento` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `rol` enum('ADMIN','USUARIO') NOT NULL DEFAULT 'USUARIO',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_usuario_correo` (`correo`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuarios`
--

LOCK TABLES `usuarios` WRITE;
/*!40000 ALTER TABLE `usuarios` DISABLE KEYS */;
INSERT INTO `usuarios` VALUES (1,'Administrador','admin@odyxs.com','admin2026','1990-01-01','Colombia','ADMIN','2026-05-17 07:38:23'),(2,'Visitante Demo','usuario@gmail.com','usuario','1998-05-15','Colombia','USUARIO','2026-05-17 07:38:23'),(3,'carlos','carlos@gmail.com','12345',NULL,NULL,'USUARIO','2026-05-17 08:09:15'),(4,'JUAN','juan@gmail.com','123456','2000-02-23','peru','USUARIO','2026-05-18 08:15:31'),(5,'jose andres','jose@gmail.com','123456','2000-03-02','Brasil','USUARIO','2026-05-18 21:15:07');
/*!40000 ALTER TABLE `usuarios` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-18 22:22:52
