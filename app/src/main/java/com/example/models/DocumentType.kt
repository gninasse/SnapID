package com.example.models

data class DocumentType(
    val id: String,
    val name: String,
    val sizeLabel: String,
    val description: String,
    val aspectRatio: Float, // width / height
    val officialSpecs: List<String>
) {
    companion object {
        val ALL = listOf(
            DocumentType(
                id = "eu_passport",
                name = "Passeport / CNI (Europe)",
                sizeLabel = "35 x 45 mm",
                description = "Format réglementaire pour la France, la Belgique et l'Union Européenne.",
                aspectRatio = 35f / 45f,
                officialSpecs = listOf(
                    "Fond uni, clair et sans ombres (gris clair ou bleu clair, blanc interdit en France)",
                    "Visage centré, tête droite, expression neutre (bouche fermée, sans sourire)",
                    "Yeux parfaitement visibles, ouverts et fixant l'objectif",
                    "Le visage doit occuper entre 70% et 80% de la hauteur de la photo"
                )
            ),
            DocumentType(
                id = "us_visa",
                name = "Visa États-Unis (USA)",
                sizeLabel = "51 x 51 mm (2x2\")",
                description = "Format standard requis pour les visas et passeports américains.",
                aspectRatio = 1.0f,
                officialSpecs = listOf(
                    "Fond blanc pur obligatoire",
                    "La tête doit occuper entre 50% et 69% de la hauteur de l'image",
                    "Photo datant de moins de 6 mois",
                    "Pas de lunettes (même de vue)"
                )
            ),
            DocumentType(
                id = "intl_standard",
                name = "Format International",
                sizeLabel = "33 x 48 mm",
                description = "Format utilisé pour les passeports chinois et de nombreux pays d'Asie.",
                aspectRatio = 33f / 48f,
                officialSpecs = listOf(
                    "Fond blanc ou bleu clair uniforme",
                    "Tête droite, oreilles dégagées et front visible",
                    "Aucun accessoire de tête (sauf motif religieux)",
                    "Pas de vêtements blancs sur fond blanc"
                )
            )
        )
    }
}
