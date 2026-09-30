package com.sukinema.init;

import com.sukinema.model.Movie;
import com.sukinema.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MovieRepository movieRepository;

    public DataInitializer(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // Los perfiles ya no se siembran: cada cuenta crea los suyos al registrarse
    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }

        List<Movie> movies = Arrays.asList(
                new Movie(
                        "Dune: Parte Dos",
                        "Paul Atreides se une a Chani y a los Fremen mientras busca venganza contra los conspiradores que destruyeron a su familia. Ante una elección entre el amor de su vida y el destino del universo, debe evitar un futuro terrible que solo él puede prever.",
                        "https://www.youtube.com/watch?v=Way9Dexny3w",
                        "https://image.tmdb.org/t/p/w1280/eZ239CUp1d6OryZEBPnO2n87gMG.jpg",
                        "https://image.tmdb.org/t/p/w500/xCHmhHeO7aOCMlzcNukGH6Q7EiD.jpg",
                        2024,
                        "99% de coincidencia",
                        "+16",
                        "Tráiler 3m 02s",
                        "Ciencia Ficción y Fantasía",
                        "Ciencia ficción, Aventura, Acción, Épica",
                        "Timothée Chalamet, Zendaya, Rebecca Ferguson, Javier Bardem",
                        "Denis Villeneuve",
                        true,
                        true
                ),
                new Movie(
                        "Stranger Things 4",
                        "Han pasado seis meses desde la batalla de Starcourt que sembró el terror en Hawkins. El grupo de amigos se separa por primera vez y las dificultades del instituto no facilitan las cosas. En este momento vulnerable surge una nueva y aterradora amenaza sobrenatural.",
                        "https://www.youtube.com/watch?v=yQEondeGvLk",
                        "https://image.tmdb.org/t/p/w1280/9P4IIMYY3HifqeruZq0ZZ9g7YUi.jpg",
                        "https://image.tmdb.org/t/p/w500/AsPD90QEQsIAtSxfSjV3fN7XFpt.jpg",
                        2022,
                        "98% de coincidencia",
                        "+16",
                        "Tráiler 3m 11s",
                        "Tendencias Ahora",
                        "Ciencia ficción, Terror, Drama, Misterio",
                        "Millie Bobby Brown, Finn Wolfhard, Winona Ryder, David Harbour",
                        "Hermanos Duffer",
                        false,
                        true
                ),
                new Movie(
                        "Cyberpunk: Edgerunners",
                        "En una distopía plagada de corrupción e implantes cibernéticos, un joven talentoso pero impulsivo de la calle intenta convertirse en un 'edgerunner': un mercenario fuera de la ley también conocido como cyberpunk.",
                        "https://www.youtube.com/watch?v=JtqIas3bYhg",
                        "https://image.tmdb.org/t/p/w1280/3UbHGmu9vIMSC5uNfnGt7DjetqT.jpg",
                        "https://image.tmdb.org/t/p/w500/7jSWOc6jWSw5hZ78HB8Hw3pJxuk.jpg",
                        2022,
                        "97% de coincidencia",
                        "+18",
                        "Tráiler 2m 58s",
                        "Anime y Animación",
                        "Anime, Cyberpunk, Acción, Ciencia ficción",
                        "KENN, Aoi Yuki, Hiroki Touchi",
                        "Hiroyuki Imaishi (Studio Trigger)",
                        false,
                        true
                ),
                new Movie(
                        "Arcane: League of Legends",
                        "En medio del conflicto entre las ciudades gemelas de Piltóver y Zaun, dos hermanas luchan en bandos opuestos de una guerra entre tecnologías mágicas y convicciones incompatibles.",
                        "https://www.youtube.com/watch?v=fXmAurh012s",
                        "https://image.tmdb.org/t/p/w1280/5cvnxEHT3e39DvT6ARw4GNCFrB0.jpg",
                        "https://image.tmdb.org/t/p/w500/fqldf2t8ztc9aiwn3k6mlX3tvRT.jpg",
                        2024,
                        "99% de coincidencia",
                        "+16",
                        "Tráiler 2m 39s",
                        "Anime y Animación",
                        "Animación, Acción, Ciencia ficción, Fantasía",
                        "Hailee Steinfeld, Ella Purnell, Katie Leung",
                        "Christian Linke, Alex Yee",
                        false,
                        true
                ),
                new Movie(
                        "Spider-Man: Cruzando el Multiverso",
                        "Miles Morales es catapultado a través del Multiverso, donde se encuentra con un equipo de Spideys encargados de proteger su propia existencia. Pero cuando los héroes chocan sobre cómo manejar una nueva amenaza, Miles debe redefinir lo que significa ser un héroe.",
                        "https://www.youtube.com/watch?v=cqGjhVJWtEg",
                        "https://image.tmdb.org/t/p/w1280/kVd3a9YeLGkoeR50jGEXM6EqseS.jpg",
                        "https://image.tmdb.org/t/p/w500/37WcNMgNOMxdhT87MFl7tq7FM1.jpg",
                        2023,
                        "98% de coincidencia",
                        "+12",
                        "Tráiler 2m 50s",
                        "Acción y Adrenalina",
                        "Animación, Acción, Aventura, Superhéroes",
                        "Shameik Moore, Hailee Steinfeld, Oscar Isaac",
                        "Joaquim Dos Santos, Kemp Powers",
                        false,
                        true
                ),
                new Movie(
                        "The Batman",
                        "En su segundo año luchando contra el crimen, Batman explora la corrupción existente en Gotham City y el vínculo de la misma con su propia familia, mientras persigue a un asesino en serie conocido como Enigma.",
                        "https://www.youtube.com/watch?v=mqqft2x_Aa4",
                        "https://image.tmdb.org/t/p/w1280/rvtdN5XkWAfGX6xDuPL6yYS2seK.jpg",
                        "https://image.tmdb.org/t/p/w500/mo7teil1qH0SxgLijnqeYP1Eb4w.jpg",
                        2022,
                        "96% de coincidencia",
                        "+16",
                        "Tráiler 2m 38s",
                        "Acción y Adrenalina",
                        "Crimen, Misterio, Suspenso, Acción",
                        "Robert Pattinson, Zoë Kravitz, Paul Dano, Colin Farrell",
                        "Matt Reeves",
                        false,
                        false
                ),
                new Movie(
                        "Oppenheimer",
                        "La historia del científico estadounidense J. Robert Oppenheimer y su papel en el desarrollo de la bomba atómica durante la Segunda Guerra Mundial en el marco del Proyecto Manhattan.",
                        "https://www.youtube.com/watch?v=uYPbbksJxIg",
                        "https://image.tmdb.org/t/p/w1280/neeNHeXjMF5fXoCJRsOmkNGC7q.jpg",
                        "https://image.tmdb.org/t/p/w500/mJRUREPjTqqMEKwEiM2sdmIGngz.jpg",
                        2023,
                        "97% de coincidencia",
                        "+16",
                        "Tráiler 3m 06s",
                        "Aclamadas por la Crítica",
                        "Biografía, Drama, Historia",
                        "Cillian Murphy, Emily Blunt, Matt Damon, Robert Downey Jr.",
                        "Christopher Nolan",
                        false,
                        false
                ),
                new Movie(
                        "Interstellar",
                        "Un grupo de científicos y exploradores, encabezados por Cooper, se embarca en un viaje espacial para encontrar un nuevo hogar para la humanidad ante la inevitable extinción en la Tierra.",
                        "https://www.youtube.com/watch?v=zSWdZVtXT7E",
                        "https://image.tmdb.org/t/p/w1280/8sNiAPPYU14PUepFNeSNGUTiHW.jpg",
                        "https://image.tmdb.org/t/p/w500/d1QKiYtceF3GDtxvTFXFAqwwah9.jpg",
                        2014,
                        "99% de coincidencia",
                        "+12",
                        "Tráiler 2m 32s",
                        "Ciencia Ficción y Fantasía",
                        "Ciencia ficción, Drama, Aventura",
                        "Matthew McConaughey, Anne Hathaway, Jessica Chastain",
                        "Christopher Nolan",
                        false,
                        false
                ),
                new Movie(
                        "John Wick 4",
                        "John Wick descubre un camino para derrotar a la Alta Mesa. Pero para poder ganar su libertad, debe enfrentarse a un nuevo enemigo con poderosas alianzas en todo el mundo y fuerzas que convierten a viejos amigos en adversarios.",
                        "https://www.youtube.com/watch?v=qEVUtrk8_B4",
                        "https://image.tmdb.org/t/p/w1280/7I6VUdPj6tQECNHdviJkUHD2u89.jpg",
                        "https://image.tmdb.org/t/p/w500/mj2Z9HnRSIEk3n7yVPoOY4Uzzfh.jpg",
                        2023,
                        "96% de coincidencia",
                        "+18",
                        "Tráiler 2m 29s",
                        "Acción y Adrenalina",
                        "Acción, Crimen, Suspenso",
                        "Keanu Reeves, Donnie Yen, Bill Skarsgård, Laurence Fishburne",
                        "Chad Stahelski",
                        false,
                        true
                ),
                new Movie(
                        "Ataque a los Titanes: Temporada Final",
                        "Eren Jaeger y los miembros del Cuerpo de Exploración libran la batalla definitiva por el destino de Paradis y de la humanidad frente al resto del mundo.",
                        "https://www.youtube.com/watch?v=M_OauHnAFc8",
                        "https://image.tmdb.org/t/p/w1280/rqbCbjB19amtOtFQbb3K2lgm2zv.jpg",
                        "https://image.tmdb.org/t/p/w500/yFPQ4JhhirnCVe2UIKGMYX7TOGZ.jpg",
                        2023,
                        "99% de coincidencia",
                        "+18",
                        "Tráiler 2m 15s",
                        "Anime y Animación",
                        "Anime, Fantasía oscura, Acción, Drama",
                        "Yuki Kaji, Yui Ishikawa, Marina Inoue, Hiroshi Kamiya",
                        "Yuichiro Hayashi (MAPPA)",
                        false,
                        false
                ),
                new Movie(
                        "Blade Runner 2049",
                        "Treinta años después de los eventos del primer film, un nuevo 'blade runner', K, descubre un secreto profundamente oculto que podría sumir lo que queda de la sociedad en el caos total.",
                        "https://www.youtube.com/watch?v=gCcx85zbxz4",
                        "https://image.tmdb.org/t/p/w1280/gNdLJU9TxrpGx4dkZidjys3fyy0.jpg",
                        "https://image.tmdb.org/t/p/w500/cOt8SQwrxpoTv9Bc3kyce3etkZX.jpg",
                        2017,
                        "95% de coincidencia",
                        "+16",
                        "Tráiler 2m 21s",
                        "Ciencia Ficción y Fantasía",
                        "Ciencia ficción, Neo-noir, Misterio",
                        "Ryan Gosling, Harrison Ford, Ana de Armas, Sylvia Hoeks",
                        "Denis Villeneuve",
                        false,
                        false
                ),
                new Movie(
                        "El Juego del Calamar",
                        "Cientos de jugadores con dificultades económicas aceptan una extraña invitación para competir en juegos infantiles. Adentro les espera un tentador premio y riesgos letales.",
                        "https://www.youtube.com/watch?v=oqxAJKy0ii4",
                        "https://image.tmdb.org/t/p/w1280/2meX1nMdScFOoV4370rqHWKmXhY.jpg",
                        "https://image.tmdb.org/t/p/w500/g72YfmqUU0AlbDXRiYmDZWs28f7.jpg",
                        2024,
                        "97% de coincidencia",
                        "+18",
                        "Tráiler 2m 10s",
                        "Tendencias Ahora",
                        "Drama, Suspenso, Supervivencia",
                        "Lee Jung-jae, Park Hae-soo, Wi Ha-joon",
                        "Hwang Dong-hyuk",
                        false,
                        true
                ),
                new Movie(
                        "Deadpool y Wolverine",
                        "Wade Wilson ve alterada su apacible vida civil cuando la Autoridad de Variación Temporal (TVA) lo recluta para una misión que requerirá formar una alianza improbable con un renuente Wolverine.",
                        "https://www.youtube.com/watch?v=73_1biulkYk",
                        "https://image.tmdb.org/t/p/w1280/by8z9Fe8y7p4jo2YlW2SZDnptyT.jpg",
                        "https://image.tmdb.org/t/p/w500/9TFSqghEHrlBMRR63yTx80Orxva.jpg",
                        2024,
                        "96% de coincidencia",
                        "+18",
                        "Tráiler 2m 38s",
                        "Acción y Adrenalina",
                        "Comedia, Acción, Superhéroes",
                        "Ryan Reynolds, Hugh Jackman, Emma Corrin",
                        "Shawn Levy",
                        false,
                        true
                ),
                new Movie(
                        "Demon Slayer: Rumbo al Entrenamiento de los Pilares",
                        "Tanjiro y el Cuerpo de Cazadores de Demonios se someten a un riguroso entrenamiento con los Pilares para prepararse para la inevitable batalla contra Muzan Kibutsuji.",
                        "https://www.youtube.com/watch?v=VQGCKyvzIM4",
                        "https://image.tmdb.org/t/p/w1280/3GQKYh6Trm8pxd2AypovoYQf4Ay.jpg",
                        "https://image.tmdb.org/t/p/w500/inXU5hvbDbitrYOgLrq2QjYqiJD.jpg",
                        2024,
                        "98% de coincidencia",
                        "+16",
                        "Tráiler 1m 55s",
                        "Anime y Animación",
                        "Anime, Acción, Fantasía, Shonen",
                        "Natsuki Hanae, Akari Kito, Hiro Shimono",
                        "Haruo Sotozaki (ufotable)",
                        false,
                        false
                ),
                new Movie(
                        "Origen (Inception)",
                        "Dom Cobb es un ladrón capaz de adentrarse en los sueños de la gente para robar sus secretos del subconsciente. Ahora se le ofrece una oportunidad de redención: en lugar de robar una idea, deberá implantarla.",
                        "https://www.youtube.com/watch?v=YoHD9XEInc0",
                        "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
                        "https://image.tmdb.org/t/p/w500/tXQvtRWfkUUnWJAn2tN3jERIUG.jpg",
                        2010,
                        "99% de coincidencia",
                        "+16",
                        "Tráiler 2m 27s",
                        "Aclamadas por la Crítica",
                        "Ciencia ficción, Acción, Suspenso, Psicológico",
                        "Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page, Tom Hardy",
                        "Christopher Nolan",
                        false,
                        false
                )
        );

        movieRepository.saveAll(movies);
        System.out.println(">>> [SUKINEMA DATA] 15 películas y trailers inicializados con éxito.");
    }
}
