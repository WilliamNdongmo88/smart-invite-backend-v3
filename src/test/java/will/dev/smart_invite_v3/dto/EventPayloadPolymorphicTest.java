package will.dev.smart_invite_v3.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import will.dev.smart_invite_v3.dto.event.request.*;
import will.dev.smart_invite_v3.enums.EventType;

import static org.junit.jupiter.api.Assertions.*;

class EventPayloadPolymorphicTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Désérialisation d'un payload Mariage")
    void testDeserializeWeddingPayload() throws Exception {
        String json = """
        {
          "eventType": "MARIAGE",
          "hero": {
            "brideFirstName": "Leatitia",
            "groomFirstName": "Christophe",
            "dateLabel": "Les 07 & 08 Août 2027",
            "venueName": "MA CABANE AU CANADA",
            "venueCity": "GOSNÉ",
            "heroCatchphrase": "Une célébration pensée comme un souvenir éternel.",
            "targetDate": "2027-08-08T14:00",
            "maxGuests": 250,
            "budget": "13 000 XAF"
          },
          "couple": {
            "bridePortraitUrl": "/images/leatitia-seule.webp",
            "brideBio": "Réservée et attentive...",
            "groomPortraitUrl": "/images/chris-seul.webp",
            "groomBio": "Christophe aime les gens...",
            "coupleTagline": "Deux façons d'être. Une seule évidence."
          },
          "story": {
            "headline": "Une histoire construite avec le temps",
            "subheadline": "Une rencontre...",
            "chapters": [
              {
                "label": "CHAPITRE I",
                "sublabel": "La rencontre",
                "year": "2008",
                "title": "Une rencontre inattendue",
                "caption": "Le premier regard",
                "image": "/images/chap1.webp",
                "paragraphs": ["Paragraphe 1"]
              }
            ],
            "footer": "Notre histoire continue"
          },
          "program": {
            "days": [
              {
                "date": "2027-08-07",
                "label": "La Veille",
                "tabIcon": "☾",
                "tabDate": "07 AOÛT",
                "tabLabel": "La Veille",
                "items": [
                  {
                    "icon": "🍽️",
                    "time": "19:00",
                    "title": "Dîner d'accueil",
                    "desc": "Repas convivial"
                  }
                ]
              }
            ],
            "footer": "Soyez à l'heure"
          },
          "dressCode": {
            "title": "Tenue d'invités",
            "description": "Chic et raffiné",
            "advice": "Éviter le blanc",
            "paletteTerracotta": [{ "color": "#C86D51", "label": "Terracotta" }],
            "paletteChampagne": [{ "color": "#F4E8C1", "label": "Champagne" }]
          },
          "faq": {
            "items": [{ "q": "Y a-t-il un parking ?", "a": "Oui" }]
          },
          "rsvp": {
            "title": "Confirmez votre présence",
            "subtitle": "Avant le 1er Juillet"
          },
          "gallery": {
            "items": [{ "url": "/images/1.webp", "caption": "Souvenir", "large": true }]
          },
          "backgrounds": {
            "hero": "/images/hero.webp",
            "venue": "/images/venue.webp",
            "quote": "/images/quote.webp",
            "galleryBand": "/images/band.webp",
            "rsvp": "/images/rsvp.webp"
          },
          "footer": {
            "logoText": "Leatitia & Christophe",
            "subText": "08 Août 2027 · Gosné",
            "loveText": "Avec tout notre amour"
          }
        }
        """;

        EventPayloadRequest request = mapper.readValue(json, EventPayloadRequest.class);

        assertInstanceOf(WeddingEventPayloadRequest.class, request);
        WeddingEventPayloadRequest wedding = (WeddingEventPayloadRequest) request;
        assertEquals(EventType.MARIAGE, wedding.eventType());
        assertEquals("Mariage Leatitia & Christophe", wedding.extractTitle());
        assertEquals("Leatitia & Christophe", wedding.extractConcernedNames());
        assertEquals(250, wedding.extractMaxGuests());
        assertEquals("13 000 XAF", wedding.extractBudget());
        assertEquals("MA CABANE AU CANADA", wedding.extractVenueName());
        assertEquals("GOSNÉ", wedding.extractVenueCity());
        assertNotNull(wedding.extractEventDate());
        assertNotNull(wedding.toContentData());
    }

    @Test
    @DisplayName("Désérialisation d'un payload Conférence")
    void testDeserializeConferencePayload() throws Exception {
        String json = """
        {
          "eventType": "CONFERENCE",
          "hero": {
            "title": "Tech Summit 2026",
            "subtitle": "L'innovation au cœur de demain",
            "edition": "5ème édition",
            "dateLabel": "15 & 16 Septembre 2026",
            "venueName": "PALAIS DES CONGRÈS",
            "venueCity": "ABIDJAN",
            "catchphrase": "Connecter les esprits, accélérer les idées.",
            "targetDate": "2026-09-15T09:00",
            "maxAttendees": 500,
            "budget": "25 000 000 XAF"
          },
          "about": {
            "headline": "Le plus grand rassemblement tech",
            "description": "Une opportunité unique de networking...",
            "stats": [
              { "value": "500+", "label": "Participants", "icon": "👥" }
            ]
          },
          "agenda": {
            "days": [
              {
                "date": "2026-09-15",
                "label": "Jour 1",
                "tabIcon": "📅",
                "tabDate": "15 SEPT",
                "tabLabel": "Jour 1",
                "sessions": [
                  {
                    "icon": "🎤",
                    "time": "09:00 - 10:00",
                    "title": "Keynote d'ouverture",
                    "speaker": "Jean Dupont",
                    "room": "Salle A",
                    "type": "keynote"
                  }
                ]
              }
            ],
            "footer": "Programme sujet à modification"
          },
          "speakers": {
            "headline": "Nos intervenants",
            "subheadline": "Des experts renommés",
            "speakers": [
              {
                "name": "Jean Dupont",
                "title": "CTO",
                "company": "Tech Corp",
                "bio": "Expert IA",
                "portraitUrl": "/images/speaker1.webp",
                "isKeynote": true
              }
            ]
          },
          "sponsors": {
            "headline": "Nos partenaires",
            "sponsors": [
              {
                "name": "Google",
                "logoUrl": "/images/google.webp",
                "level": "platine",
                "websiteUrl": "https://google.com"
              }
            ]
          },
          "faq": {
            "items": [{ "q": "Comment obtenir son badge ?", "a": "À l'entrée" }]
          },
          "rsvp": {
            "title": "Réservez votre place",
            "subtitle": "Places limitées"
          },
          "gallery": {
            "items": [{ "url": "/images/conf1.webp", "caption": "Édition 2025", "large": false }]
          },
          "backgrounds": {
            "hero": "/images/conf-hero.webp",
            "about": "/images/conf-about.webp",
            "agendaBand": "/images/conf-agenda.webp",
            "rsvp": "/images/conf-rsvp.webp"
          },
          "footer": {
            "logoText": "Tech Summit 2026",
            "subText": "15 & 16 Sept · Abidjan",
            "tagline": "L'EXCELLENCE RÉUNIT LES ESPRITS"
          }
        }
        """;

        EventPayloadRequest request = mapper.readValue(json, EventPayloadRequest.class);

        assertInstanceOf(ConferenceEventPayloadRequest.class, request);
        ConferenceEventPayloadRequest conf = (ConferenceEventPayloadRequest) request;
        assertEquals(EventType.CONFERENCE, conf.eventType());
        assertEquals("Tech Summit 2026", conf.extractTitle());
        assertEquals(500, conf.extractMaxGuests());
        assertEquals("25 000 000 XAF", conf.extractBudget());
        assertEquals("PALAIS DES CONGRÈS", conf.extractVenueName());
        assertEquals("ABIDJAN", conf.extractVenueCity());
        assertNotNull(conf.extractEventDate());
        assertNotNull(conf.toContentData());
    }

    @Test
    @DisplayName("Désérialisation d'un payload Gala")
    void testDeserializeGalaPayload() throws Exception {
        String json = """
        {
          "eventType": "GALA",
          "hero": {
            "title": "Gala de Charité 2026",
            "subtitle": "Une soirée d'exception",
            "edition": "3ème édition",
            "dateLabel": "Samedi 28 Novembre 2026",
            "venueName": "GRAND HÔTEL IVOIRE",
            "venueCity": "ABIDJAN",
            "catchphrase": "Élégance, générosité et partage.",
            "dressCode": "Tenue de soirée",
            "targetDate": "2026-11-28T20:00",
            "maxGuests": 300,
            "budget": "50 000 000 XAF"
          },
          "about": {
            "headline": "Au profit de l'éducation",
            "description": "100% des bénéfices...",
            "cause": "Éducation pour tous",
            "causeIcon": "🎗️"
          },
          "program": {
            "headline": "Déroulement de la soirée",
            "items": [
              { "icon": "🥂", "time": "20:00", "title": "Cocktail de bienvenue", "desc": "Accueil VIP" }
            ],
            "footer": "Fin de soirée à 02h00"
          },
          "performers": {
            "headline": "Artistes invités",
            "subheadline": "Prestations en direct",
            "performers": [
              { "name": "Orchestre National", "role": "Musique", "bio": "Symphonie classique", "portraitUrl": "/images/art1.webp" }
            ]
          },
          "dressCode": {
            "title": "Dress Code",
            "description": "Black Tie",
            "advice": "Smoking ou robe longue",
            "swatches": [{ "color": "#000000", "label": "Noir" }]
          },
          "faq": {
            "items": [{ "q": "Placement à table ?", "a": "Par table assignée" }]
          },
          "rsvp": {
            "title": "Confirmez votre présence",
            "subtitle": "Tables limitées"
          },
          "gallery": {
            "items": []
          },
          "backgrounds": {
            "hero": "/images/gala-hero.webp",
            "about": "/images/gala-about.webp",
            "programBand": "/images/gala-band.webp",
            "rsvp": "/images/gala-rsvp.webp"
          },
          "footer": {
            "logoText": "Gala de Charité 2026",
            "subText": "Grand Hôtel Ivoire",
            "tagline": "UNE SOIRÉE, UNE CAUSE"
          }
        }
        """;

        EventPayloadRequest request = mapper.readValue(json, EventPayloadRequest.class);

        assertInstanceOf(GalaEventPayloadRequest.class, request);
        GalaEventPayloadRequest gala = (GalaEventPayloadRequest) request;
        assertEquals(EventType.GALA, gala.eventType());
        assertEquals("Gala de Charité 2026", gala.extractTitle());
        assertEquals(300, gala.extractMaxGuests());
        assertEquals("GRAND HÔTEL IVOIRE", gala.extractVenueName());
        assertNotNull(gala.toContentData());
    }

    @Test
    @DisplayName("Désérialisation d'un payload Cérémonie")
    void testDeserializeCeremoniePayload() throws Exception {
        String json = """
        {
          "eventType": "CEREMONIE",
          "hero": {
            "type": "BAPTÊME",
            "title": "Baptême de Léo",
            "subtitle": "Une journée de grâce et de partage",
            "dateLabel": "Dimanche 12 Juillet 2026",
            "venueName": "PAROISSE NOTRE DAME",
            "venueCity": "PARIS",
            "catchphrase": "Que sa vie soit bénie.",
            "targetDate": "2026-07-12T10:30",
            "maxGuests": 80,
            "budget": "3 500 EUR"
          },
          "about": {
            "headline": "Un moment sacré",
            "description": "Nous serions très honorés de votre présence...",
            "message": "Célébrons ensemble",
            "messageIcon": "🕊️"
          },
          "program": {
            "headline": "Moments forts",
            "moments": [
              { "icon": "⛪", "time": "10:30", "title": "Messe de baptême", "desc": "Célébration à l'église" }
            ],
            "footer": "Déjeuner à suivre"
          },
          "keyGuests": {
            "headline": "Parrain & Marraine",
            "subheadline": "Guides et protecteurs",
            "guests": [
              { "name": "Marc", "role": "Parrain", "bio": "Oncle dévoué", "portraitUrl": "/images/marc.webp" }
            ]
          },
          "dressCode": {
            "title": "Tenue de circonstance",
            "description": "Élégante et sobre",
            "advice": "Tons pastels recommandés",
            "swatches": [{ "color": "#E6F0FA", "label": "Bleu ciel" }]
          },
          "faq": {
            "items": []
          },
          "rsvp": {
            "title": "Merci de confirmer votre présence",
            "subtitle": "Avant le 1er Juin"
          },
          "gallery": {
            "items": []
          },
          "backgrounds": {
            "hero": "/images/cerem-hero.webp",
            "about": "/images/cerem-about.webp",
            "programBand": "/images/cerem-band.webp",
            "rsvp": "/images/cerem-rsvp.webp"
          },
          "footer": {
            "logoText": "Baptême de Léo",
            "subText": "12 Juillet 2026",
            "tagline": "AMOUR ET BÉNÉDICTION"
          }
        }
        """;

        EventPayloadRequest request = mapper.readValue(json, EventPayloadRequest.class);

        assertInstanceOf(CeremonieEventPayloadRequest.class, request);
        CeremonieEventPayloadRequest cerem = (CeremonieEventPayloadRequest) request;
        assertEquals(EventType.CEREMONIE, cerem.eventType());
        assertEquals("Baptême de Léo", cerem.extractTitle());
        assertEquals(80, cerem.extractMaxGuests());
        assertEquals("PAROISSE NOTRE DAME", cerem.extractVenueName());
        assertNotNull(cerem.toContentData());
    }
}
