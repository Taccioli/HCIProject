# AirsimTracker
## Introduzione all'applicazione
AirsimTracker è una applicazione Android che permette di controllare autonomamente un veicolo all'interno dell'ambiente Airsim, con l'obiettivo di inseguire un'altra automobile che può essere pilotata manualmente o da altri dispositivi.

## Funzionamento
All'avvio, l'applicazione si collega autonomamente a una istanza di Airsim aperta, prendendo possesso di uno dei veicoli presenti nell'ambiente.
Utilizzando le immagini fornite dalle telecamere virtuali del veicolo, è possibile effettuare una Single Shot Detection per verificare la presenza di un'automobile all'interno del campo visivo del veicolo pilotato e di stimarne la posizione relativa, utilizzando per questo anche dati sulla profondità di ciò che l'agente sta visualizzando.
Appresi questi dati, il veicolo è in grado di decidere in autonomia in che direzione muoversi e di fermarsi ogni qualvolta si avvicini troppo all'obiettivo.
