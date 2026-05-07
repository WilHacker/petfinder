package com.frontend.petfinder.core.domain

enum class EstadoMascota(val valor: String) {
    EN_CASA("en_casa"),
    EN_PASEO("en_paseo"),
    EXTRAVIADA("extraviada"),
    RECUPERADA("recuperada")
}

enum class TipoContacto(val valor: String) {
    WHATSAPP("WhatsApp"),
    CELULAR("Celular"),
    FIJO("Fijo"),
    TELEGRAM("Telegram")
}

enum class RelacionPropietario(val valor: String) {
    DUENO_PRINCIPAL("Dueño Principal"),
    FAMILIAR("Familiar"),
    CUIDADOR("Cuidador")
}