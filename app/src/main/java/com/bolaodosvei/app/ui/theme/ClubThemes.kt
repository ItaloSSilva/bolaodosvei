package com.bolaodosvei.app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bolaodosvei.app.R
import com.bolaodosvei.app.model.League

enum class ClubBackgroundPattern {
    CLEAN_DARK,
    HORIZONTAL_HOOPS,
    VERTICAL_STRIPES,
    DIAGONAL_SASH,
    TRICOLOR_BANDS,
    ROYAL_CROWN_RAYS,
    STAR_CONSTELLATION,
    PALM_WAVES,
    DIAMOND_CHECKER,
    STADIUM_LIGHTS
}

enum class ClubMascotType(@DrawableRes val drawableRes: Int? = null) {
    NONE(null),
    URUBU(R.drawable.mascot_urubu),
    PORCO(R.drawable.mascot_porco),
    BIRIBA(R.drawable.mascot_biriba),
    SANTO_PAULO(R.drawable.mascot_santo_paulo),
    GAVIAO(R.drawable.mascot_gaviao),
    GALO_DOIDO(R.drawable.mascot_galo),
    SACI(R.drawable.mascot_saci),
    MOSQUETEIRO(R.drawable.mascot_mosqueteiro),
    RAPOSA(R.drawable.mascot_raposa),
    GUERREIRO(R.drawable.mascot_guerreiro),
    ALMIRANTE(R.drawable.mascot_almirante),
    SUPER_HEROI(R.drawable.mascot_super_heroi),
    LEAO_TRICOLOR(R.drawable.mascot_leao_tricolor),
    FURACAO(R.drawable.mascot_furacao),
    BALEIA(R.drawable.mascot_baleia),
    TOURO(R.drawable.mascot_touro),
    PERIQUITO(R.drawable.mascot_periquito),
    DOURADO(R.drawable.mascot_dourado),
    TIGRE(R.drawable.mascot_tigre),
    CANHAO(R.drawable.mascot_canhao),
    LIVER_BIRD(R.drawable.mascot_liver_bird),
    CARAVELA(R.drawable.mascot_caravela),
    LEAO_AZUL(R.drawable.mascot_leao_azul),
    DIABO_VERMELHO(R.drawable.mascot_diabo_vermelho),
    GALO_SPURS(R.drawable.mascot_galo_spurs),
    GAIVOTA(R.drawable.mascot_gaivota),
    MARTELOS(R.drawable.mascot_martelos),
    AGUIA(R.drawable.mascot_aguia),
    TEXUGO(R.drawable.mascot_texugo),
    ABELHA(R.drawable.mascot_abelha),
    CEREJAS(R.drawable.mascot_cerejas),
    TORRE(R.drawable.mascot_torre),
    ARVORE(R.drawable.mascot_arvore),
    LOBO(R.drawable.mascot_lobo),
    CAVALO(R.drawable.mascot_cavalo),
    SANTO_HALO(R.drawable.mascot_santo_halo),
    COROA_REAL(R.drawable.mascot_coroa_real),
    DRAGAO(R.drawable.mascot_dragao),
    URSO(R.drawable.mascot_urso),
    SUBMARINO(R.drawable.mascot_submarino),
    MORCEGO(R.drawable.mascot_morcego),
    RAIO(R.drawable.mascot_raio),
    CANARIO(R.drawable.mascot_canario),
    PEPINO(R.drawable.mascot_pepino)
}

data class AppThemePreset(
    val id: String,
    val name: String,
    val mascotName: String,
    val mascotType: ClubMascotType,
    val pattern: ClubBackgroundPattern,
    val league: League?,
    val logoUrl: String,
    val isLightTheme: Boolean = false,
    val gradientTopHex: Long,
    val gradientMidHex: Long,
    val gradientBottomHex: Long,
    val accentHex: Long,
    val secondaryHex: Long = 0xFFFFD600L,
    val surfaceHex: Long = if (isLightTheme) 0xFFF3F4F6L else 0xFF10121AL,
    val cardHex: Long = if (isLightTheme) 0xFFFFFFFFL else 0xFF171A24L,
    val borderHex: Long = if (isLightTheme) 0xFFD4C59AL else 0xFF282D3DL,
    val textPrimaryHex: Long = if (isLightTheme) 0xFF111827L else 0xFFF5F7FAL,
    val textSecondaryHex: Long = if (isLightTheme) 0xFF4B5563L else 0xFF98A0B3L
) {
    val backgroundBrush: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(gradientTopHex),
                Color(gradientMidHex),
                Color(gradientBottomHex)
            )
        )

    val accentColor: Color get() = Color(accentHex)
    val secondaryColor: Color get() = Color(secondaryHex)
    val surfaceColor: Color get() = Color(surfaceHex)
    val cardColor: Color get() = Color(cardHex)
    val borderColor: Color get() = Color(borderHex)
    val textPrimaryColor: Color get() = Color(textPrimaryHex)
    val textSecondaryColor: Color get() = Color(textSecondaryHex)
}

private fun crest(id: Int): String = "https://a.espncdn.com/i/teamlogos/soccer/500/$id.png"

private fun darkClub(
    id: String,
    name: String,
    mascotName: String,
    mascotType: ClubMascotType,
    pattern: ClubBackgroundPattern,
    league: League,
    espnId: Int,
    top: Long,
    mid: Long,
    bottom: Long,
    accent: Long,
    secondary: Long = 0xFFFFD600L,
    card: Long = 0xFF161923L,
    border: Long = 0xFF2C3142L
) = AppThemePreset(
    id = id,
    name = name,
    mascotName = mascotName,
    mascotType = mascotType,
    pattern = pattern,
    league = league,
    logoUrl = crest(espnId),
    isLightTheme = false,
    gradientTopHex = top,
    gradientMidHex = mid,
    gradientBottomHex = bottom,
    accentHex = accent,
    secondaryHex = secondary,
    surfaceHex = top,
    cardHex = card,
    borderHex = border,
    textPrimaryHex = 0xFFF5F7FAL,
    textSecondaryHex = 0xFFA3ACC0L
)

private fun lightClub(
    id: String,
    name: String,
    mascotName: String,
    mascotType: ClubMascotType,
    pattern: ClubBackgroundPattern,
    league: League,
    espnId: Int,
    top: Long,
    mid: Long,
    bottom: Long,
    accent: Long,
    secondary: Long,
    card: Long = 0xFFFFFFFFL,
    border: Long = 0xFFD9CBA0L
) = AppThemePreset(
    id = id,
    name = name,
    mascotName = mascotName,
    mascotType = mascotType,
    pattern = pattern,
    league = league,
    logoUrl = crest(espnId),
    isLightTheme = true,
    gradientTopHex = top,
    gradientMidHex = mid,
    gradientBottomHex = bottom,
    accentHex = accent,
    secondaryHex = secondary,
    surfaceHex = mid,
    cardHex = card,
    borderHex = border,
    textPrimaryHex = 0xFF121520L,
    textSecondaryHex = 0xFF525A6EL
)

val DefaultAppTheme = AppThemePreset(
    id = "default_black",
    name = "Preto Gradiente",
    mascotName = "Padrão Bolão dos Véi",
    mascotType = ClubMascotType.NONE,
    pattern = ClubBackgroundPattern.CLEAN_DARK,
    league = null,
    logoUrl = "",
    isLightTheme = false,
    gradientTopHex = 0xFF050507L,
    gradientMidHex = 0xFF0D0F16L,
    gradientBottomHex = 0xFF151822L,
    accentHex = 0xFF00E676L,
    secondaryHex = 0xFFFFD600L
)

object ClubThemesCatalog {

    val allThemes: List<AppThemePreset> = listOf(
        DefaultAppTheme,

        // ================= BRASILEIRÃO SÉRIE A (20 clubes autênticos) =================
        darkClub(
            "bra_fla", "Flamengo", "Urubu • Rubro-Negro", ClubMascotType.URUBU,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.BRASILEIRAO, 819,
            0xFF0C0405L, 0xFF2E070BL, 0xFF4A0A10L, 0xFFFF2E3BL, 0xFFFFD600L, 0xFF1D0A0CL, 0xFF5E171EL
        ),
        darkClub(
            "bra_pal", "Palmeiras", "Porco Gobatto • Alviverde", ClubMascotType.PORCO,
            ClubBackgroundPattern.PALM_WAVES, League.BRASILEIRAO, 2029,
            0xFF04140AL, 0xFF0A331AL, 0xFF124F2AL, 0xFF00E676L, 0xFFF8FAFCL, 0xFF0A2414L, 0xFF1E5C35L
        ),
        darkClub(
            "bra_bot", "Botafogo", "Biriba • Estrela Solitária", ClubMascotType.BIRIBA,
            ClubBackgroundPattern.STAR_CONSTELLATION, League.BRASILEIRAO, 6086,
            0xFF050506L, 0xFF14161BL, 0xFF232730L, 0xFFFFFFFFL, 0xFFFFD600L, 0xFF121419L, 0xFF3A404DL
        ),
        lightClub(
            "bra_sao", "São Paulo", "Santo Paulo • Tricolor", ClubMascotType.SANTO_PAULO,
            ClubBackgroundPattern.TRICOLOR_BANDS, League.BRASILEIRAO, 2026,
            0xFFFAFAFAL, 0xFFF2ECECL, 0xFFE8DDDDL, 0xFFD32F2FL, 0xFF111111L, 0xFFFFFFFFL, 0xFFE0B4B4L
        ),
        darkClub(
            "bra_cor", "Corinthians", "Gavião • Timão", ClubMascotType.GAVIAO,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.BRASILEIRAO, 874,
            0xFF070709L, 0xFF17181EL, 0xFF262832L, 0xFFF8FAFCL, 0xFFEF4444L, 0xFF14151AL, 0xFF3E4250L
        ),
        darkClub(
            "bra_cam", "Atlético-MG", "Galo Doido", ClubMascotType.GALO_DOIDO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.BRASILEIRAO, 7632,
            0xFF060608L, 0xFF16181FL, 0xFF252833L, 0xFFF8FAFCL, 0xFFFFD600L, 0xFF13151BL, 0xFF3A3F4DL
        ),
        darkClub(
            "bra_int", "Internacional", "Saci • Colorado", ClubMascotType.SACI,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.BRASILEIRAO, 1936,
            0xFF180406L, 0xFF3E0A10L, 0xFF5C0F18L, 0xFFFF3B47L, 0xFFFFFFFFL, 0xFF26090CL, 0xFF6B1A23L
        ),
        darkClub(
            "bra_gre", "Grêmio", "Mosqueteiro • Imortal", ClubMascotType.MOSQUETEIRO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.BRASILEIRAO, 6273,
            0xFF04101AL, 0xFF0A2A45L, 0xFF114069L, 0xFF38BDF8L, 0xFFFFFFFFL, 0xFF0A1D2EL, 0xFF1E5482L
        ),
        darkClub(
            "bra_cru", "Cruzeiro", "Raposa • Cabuloso", ClubMascotType.RAPOSA,
            ClubBackgroundPattern.STAR_CONSTELLATION, League.BRASILEIRAO, 2022,
            0xFF050C21L, 0xFF0B1F54L, 0xFF123285L, 0xFF60A5FAL, 0xFFFFFFFFL, 0xFF0B183DL, 0xFF244BB0L
        ),
        darkClub(
            "bra_flu", "Fluminense", "Guerreiro Tricolor", ClubMascotType.GUERREIRO,
            ClubBackgroundPattern.TRICOLOR_BANDS, League.BRASILEIRAO, 3445,
            0xFF1A070CL, 0xFF360E18L, 0xFF0E3320L, 0xFF10B981L, 0xFFF43F5EL, 0xFF210D13L, 0xFF4F2230L
        ),
        lightClub(
            "bra_vas", "Vasco", "Almirante • Cruzmaltino", ClubMascotType.ALMIRANTE,
            ClubBackgroundPattern.DIAGONAL_SASH, League.BRASILEIRAO, 3454,
            0xFFFAFAFAL, 0xFFF0F1F4L, 0xFFE3E5EBL, 0xFF111827L, 0xFFDC2626L, 0xFFFFFFFFL, 0xFFCBD5E1L
        ),
        darkClub(
            "bra_bah", "Bahia", "Super-Homem • Esquadrão", ClubMascotType.SUPER_HEROI,
            ClubBackgroundPattern.TRICOLOR_BANDS, League.BRASILEIRAO, 9967,
            0xFF061026L, 0xFF0D275CL, 0xFF3D0E1EL, 0xFF38BDF8L, 0xFFFF4757L, 0xFF0D1B3DL, 0xFF26498CL
        ),
        darkClub(
            "bra_cap", "Athletico-PR", "Furacão • Rubro-Negro", ClubMascotType.FURACAO,
            ClubBackgroundPattern.DIAGONAL_SASH, League.BRASILEIRAO, 3458,
            0xFF0F0405L, 0xFF30080CL, 0xFF4D0D13L, 0xFFFF2E3BL, 0xFFF8FAFCL, 0xFF1D090BL, 0xFF5C1920L
        ),
        lightClub(
            "bra_san", "Santos", "Baleia • Peixe da Vila", ClubMascotType.BALEIA,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.BRASILEIRAO, 2674,
            0xFFFFFFFFL, 0xFFF4F4F0L, 0xFFEAE6D8L, 0xFFD4AF37L, 0xFF111827L, 0xFFFFFFFFL, 0xFFD9CCA0L
        ),
        lightClub(
            "bra_rbb", "RB Bragantino", "Touro Loko • Massa Bruta", ClubMascotType.TOURO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.BRASILEIRAO, 6079,
            0xFFFAFAFAL, 0xFFF5EDF0L, 0xFFEBDFE4L, 0xFFE11D48L, 0xFF1E293BL, 0xFFFFFFFFL, 0xFFE2C3CCL
        ),
        darkClub(
            "bra_vit", "Vitória", "Leão da Barra", ClubMascotType.LEAO_TRICOLOR,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.BRASILEIRAO, 3457,
            0xFF100405L, 0xFF33070BL, 0xFF4F0B11L, 0xFFEF4444L, 0xFFF8FAFCL, 0xFF1F090BL, 0xFF611920L
        ),
        darkClub(
            "bra_cha", "Chapecoense", "Verdão do Oeste • Índio Condá", ClubMascotType.GUERREIRO,
            ClubBackgroundPattern.PALM_WAVES, League.BRASILEIRAO, 9318,
            0xFF04140AL, 0xFF0B351BL, 0xFF13522BL, 0xFF22C55EL, 0xFFFFFFFFL, 0xFF0A2414L, 0xFF1F6137L
        ),
        darkClub(
            "bra_rem", "Remo", "Leão Azul • Rei da Amazônia", ClubMascotType.LEAO_AZUL,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.BRASILEIRAO, 4936,
            0xFF040A18L, 0xFF0A1B3FL, 0xFF102A61L, 0xFF60A5FAL, 0xFFFFFFFFL, 0xFF091633L, 0xFF214696L
        ),
        darkClub(
            "bra_mir", "Mirassol", "Leão • Amarelo & Verde", ClubMascotType.LEAO_TRICOLOR,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.BRASILEIRAO, 9169,
            0xFF141103L, 0xFF2E2707L, 0xFF0E2F18L, 0xFFFACC15L, 0xFF22C55EL, 0xFF1F1B07L, 0xFF4F4614L
        ),
        lightClub(
            "bra_cfc", "Coritiba", "Coxa-Branca • Pinheiro", ClubMascotType.ARVORE,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.BRASILEIRAO, 3456,
            0xFFFAFAFAL, 0xFFEEF7F1L, 0xFFDCF0E2L, 0xFF047857L, 0xFF064E3BL, 0xFFFFFFFFL, 0xFFB7DEC4L
        ),

        // ================= PREMIER LEAGUE (20 clubes autênticos) =================
        darkClub(
            "eng_ars", "Arsenal", "Canhão • The Gunners", ClubMascotType.CANHAO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.PREMIER_LEAGUE, 359,
            0xFF170507L, 0xFF3D0C12L, 0xFF59111AL, 0xFFFF3B47L, 0xFFFFD600L, 0xFF240B0EL, 0xFF691D26L
        ),
        darkClub(
            "eng_liv", "Liverpool", "Liver Bird • The Reds", ClubMascotType.LIVER_BIRD,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.PREMIER_LEAGUE, 364,
            0xFF1A0407L, 0xFF450A12L, 0xFF630E1AL, 0xFFFF2E43L, 0xFF2DD4BFL, 0xFF29090DL, 0xFF701B26L
        ),
        darkClub(
            "eng_mci", "Man City", "Caravela • Sky Blues", ClubMascotType.CARAVELA,
            ClubBackgroundPattern.STAR_CONSTELLATION, League.PREMIER_LEAGUE, 382,
            0xFF061624L, 0xFF0E3554L, 0xFF175280L, 0xFF56CCF2L, 0xFFFFFFFFL, 0xFF0C253BL, 0xFF28699CL
        ),
        darkClub(
            "eng_che", "Chelsea", "Leão Azul • The Blues", ClubMascotType.LEAO_AZUL,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.PREMIER_LEAGUE, 363,
            0xFF050E26L, 0xFF0B235EL, 0xFF12368FL, 0xFF3B82F6L, 0xFFFFD600L, 0xFF0B1A42L, 0xFF234CB0L
        ),
        darkClub(
            "eng_mun", "Man United", "Red Devil • Tridente", ClubMascotType.DIABO_VERMELHO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.PREMIER_LEAGUE, 360,
            0xFF170505L, 0xFF3D0D0DL, 0xFF591212L, 0xFFEF4444L, 0xFFFACC15L, 0xFF240B0BL, 0xFF6B1E1EL
        ),
        lightClub(
            "eng_tot", "Tottenham", "Galo Spurs • Lilywhites", ClubMascotType.GALO_SPURS,
            ClubBackgroundPattern.DIAGONAL_SASH, League.PREMIER_LEAGUE, 367,
            0xFFFFFFFFL, 0xFFF1F4F9L, 0xFFE2E8F2L, 0xFF132257L, 0xFF3B82F6L, 0xFFFFFFFFL, 0xFFCBD5E1L
        ),
        darkClub(
            "eng_new", "Newcastle", "Pega • The Magpies", ClubMascotType.GAVIAO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 361,
            0xFF07080BL, 0xFF171A22L, 0xFF252A36L, 0xFFFFFFFFL, 0xFF38BDF8L, 0xFF14161DL, 0xFF3E4454L
        ),
        darkClub(
            "eng_avl", "Aston Villa", "Leão • Claret & Blue", ClubMascotType.LEAO_AZUL,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.PREMIER_LEAGUE, 362,
            0xFF1A0714L, 0xFF42102FL, 0xFF163554L, 0xFF67C6FFL, 0xFFF43F5EL, 0xFF260D1EL, 0xFF612349L
        ),
        darkClub(
            "eng_bha", "Brighton", "Gaivota • The Seagulls", ClubMascotType.GAIVOTA,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 331,
            0xFF051326L, 0xFF0B2B59L, 0xFF124287L, 0xFF38BDF8L, 0xFFFFFFFFL, 0xFF0A1F40L, 0xFF2255A3L
        ),
        darkClub(
            "eng_whu", "West Ham", "Martelos • The Hammers", ClubMascotType.MARTELOS,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.PREMIER_LEAGUE, 371,
            0xFF1C0813L, 0xFF47122EL, 0xFF61193EL, 0xFF38BDF8L, 0xFFF43F5EL, 0xFF2B0E1EL, 0xFF6E244AL
        ),
        darkClub(
            "eng_cry", "Crystal Palace", "Águia • The Eagles", ClubMascotType.AGUIA,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 384,
            0xFF071129L, 0xFF132C66L, 0xFF4D101EL, 0xFF3B82F6L, 0xFFEF4444L, 0xFF101F47L, 0xFF3152A3L
        ),
        lightClub(
            "eng_ful", "Fulham", "Texugo • Cottagers", ClubMascotType.TEXUGO,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.PREMIER_LEAGUE, 370,
            0xFFFFFFFFL, 0xFFF3F4F6L, 0xFFE5E7EBL, 0xFF111827L, 0xFFEF4444L, 0xFFFFFFFFL, 0xFFD1D5DBL
        ),
        darkClub(
            "eng_bre", "Brentford", "Abelha • The Bees", ClubMascotType.ABELHA,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 337,
            0xFF170507L, 0xFF3D0C12L, 0xFF541018L, 0xFFEF4444L, 0xFFFACC15L, 0xFF240B0EL, 0xFF661E26L
        ),
        darkClub(
            "eng_bou", "Bournemouth", "Cerejas • The Cherries", ClubMascotType.CEREJAS,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 349,
            0xFF140406L, 0xFF38090FL, 0xFF520D16L, 0xFFEF4444L, 0xFFFACC15L, 0xFF21090CL, 0xFF611B24L
        ),
        darkClub(
            "eng_eve", "Everton", "Torre • The Toffees", ClubMascotType.TORRE,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.PREMIER_LEAGUE, 368,
            0xFF050E26L, 0xFF0B225EL, 0xFF11358FL, 0xFF3B82F6L, 0xFFFFFFFFL, 0xFF0A1A42L, 0xFF2149A8L
        ),
        darkClub(
            "eng_nfo", "Nottm Forest", "Carvalho • Tricky Trees", ClubMascotType.ARVORE,
            ClubBackgroundPattern.STAR_CONSTELLATION, League.PREMIER_LEAGUE, 393,
            0xFF1A0508L, 0xFF420B14L, 0xFF5E0F1CL, 0xFFEF4444L, 0xFFFFFFFFL, 0xFF260A0FL, 0xFF6E1D2BL
        ),
        darkClub(
            "eng_wol", "Wolves", "Lobo • Old Gold", ClubMascotType.LOBO,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.PREMIER_LEAGUE, 380,
            0xFF171104L, 0xFF3D2D08L, 0xFF59410AL, 0xFFF59E0BL, 0xFFF8FAFCL, 0xFF241B08L, 0xFF6B5014L
        ),
        darkClub(
            "eng_lei", "Leicester", "Raposa • The Foxes", ClubMascotType.RAPOSA,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.PREMIER_LEAGUE, 375,
            0xFF061029L, 0xFF0D2663L, 0xFF143A96L, 0xFF3B82F6L, 0xFFFACC15L, 0xFF0C1D47L, 0xFF254EB5L
        ),
        darkClub(
            "eng_ips", "Ipswich", "Cavalo • Tractor Boys", ClubMascotType.CAVALO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.PREMIER_LEAGUE, 373,
            0xFF061026L, 0xFF0E265CL, 0xFF163A8CL, 0xFF3B82F6L, 0xFFFFFFFFL, 0xFF0C1D42L, 0xFF264DA8L
        ),
        darkClub(
            "eng_sou", "Southampton", "Asas • The Saints", ClubMascotType.SANTO_HALO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.PREMIER_LEAGUE, 376,
            0xFF170507L, 0xFF3D0C12L, 0xFF571019L, 0xFFEF4444L, 0xFFFACC15L, 0xFF240B0EL, 0xFF691E27L
        ),

        // ================= LA LIGA (20 clubes autênticos) =================
        lightClub(
            "esp_rma", "Real Madrid", "Coroa Real • Merengues", ClubMascotType.COROA_REAL,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.LA_LIGA, 86,
            0xFFFFFEFAL, 0xFFF8F4E6L, 0xFFEDE4C8L, 0xFFC99A1EL, 0xFF1E3A8AL, 0xFFFFFFFFL, 0xFFDEC98AL
        ),
        darkClub(
            "esp_bar", "Barcelona", "Dragão • Blaugrana", ClubMascotType.DRAGAO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 83,
            0xFF081029L, 0xFF162763L, 0xFF4D0E26L, 0xFF3B82F6L, 0xFFE11D48L, 0xFF121C42L, 0xFF3B438CL
        ),
        darkClub(
            "esp_atm", "Atlético Madrid", "Urso de Madri • Colchonero", ClubMascotType.URSO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 1068,
            0xFF17060CL, 0xFF3D0F1EL, 0xFF122354L, 0xFFEF4444L, 0xFF60A5FAL, 0xFF240D18L, 0xFF5E223AL
        ),
        darkClub(
            "esp_ath", "Athletic Club", "Leão de San Mamés", ClubMascotType.LEAO_TRICOLOR,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 93,
            0xFF1A0507L, 0xFF420B11L, 0xFF5E0F17L, 0xFFEF4444L, 0xFFFFFFFFL, 0xFF260A0DL, 0xFF6B1D26L
        ),
        lightClub(
            "esp_vil", "Villarreal", "Submarino Amarelo", ClubMascotType.SUBMARINO,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.LA_LIGA, 102,
            0xFFFFFCE6L, 0xFFFFF5B8L, 0xFFFCEB8BL, 0xFF1D4ED8L, 0xFFD97706L, 0xFFFFFFFFL, 0xFFE5C85CL
        ),
        darkClub(
            "esp_bet", "Real Betis", "Coroa • Verdiblancos", ClubMascotType.COROA_REAL,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 244,
            0xFF04170BL, 0xFF0B3B1DL, 0xFF125C2EL, 0xFF22C55EL, 0xFFFFFFFFL, 0xFF0A2614L, 0xFF1E6B3CL
        ),
        lightClub(
            "esp_rso", "Real Sociedad", "Coroa • Txuri-Urdin", ClubMascotType.COROA_REAL,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 89,
            0xFFF8FBFFL, 0xFFEBF4FFL, 0xFFDCEBFFL, 0xFF0284C7L, 0xFF1E3A8AL, 0xFFFFFFFFL, 0xFFBAD9F7L
        ),
        lightClub(
            "esp_sev", "Sevilla", "Realeza de Nervión", ClubMascotType.COROA_REAL,
            ClubBackgroundPattern.ROYAL_CROWN_RAYS, League.LA_LIGA, 243,
            0xFFFFFAFAL, 0xFFFAF0F0L, 0xFFF2E2E2L, 0xFFDC2626L, 0xFFB45309L, 0xFFFFFFFFL, 0xFFE8C1C1L
        ),
        lightClub(
            "esp_val", "Valencia", "Morcego de Mestalla", ClubMascotType.MORCEGO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.LA_LIGA, 94,
            0xFFFFFDF9L, 0xFFFFF4E6L, 0xFFFCE7CCL, 0xFFEA580CL, 0xFF111827L, 0xFFFFFFFFL, 0xFFF0C99CL
        ),
        darkClub(
            "esp_gir", "Girona", "Mosca • Blanquivermells", ClubMascotType.ABELHA,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 9812,
            0xFF170507L, 0xFF3D0C12L, 0xFF571019L, 0xFFEF4444L, 0xFFFFFFFFL, 0xFF240B0EL, 0xFF691D26L
        ),
        lightClub(
            "esp_cel", "Celta de Vigo", "Caravela da Galícia", ClubMascotType.CARAVELA,
            ClubBackgroundPattern.PALM_WAVES, League.LA_LIGA, 85,
            0xFFF5FBFFL, 0xFFE3F4FFL, 0xFFCFEAFFL, 0xFF0284C7L, 0xFFE11D48L, 0xFFFFFFFFL, 0xFFAAD8F5L
        ),
        darkClub(
            "esp_osa", "Osasuna", "Touro de Pamplona", ClubMascotType.TOURO,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.LA_LIGA, 97,
            0xFF17060CL, 0xFF3D0E1EL, 0xFF14214DL, 0xFFEF4444L, 0xFF60A5FAL, 0xFF240C17L, 0xFF5C1E38L
        ),
        darkClub(
            "esp_mal", "Mallorca", "Dimoni • Bermellones", ClubMascotType.DIABO_VERMELHO,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.LA_LIGA, 84,
            0xFF1A0507L, 0xFF420B11L, 0xFF5C0F17L, 0xFFEF4444L, 0xFFFACC15L, 0xFF260A0DL, 0xFF6B1E26L
        ),
        lightClub(
            "esp_ray", "Rayo Vallecano", "Raio • Vallecas", ClubMascotType.RAIO,
            ClubBackgroundPattern.DIAGONAL_SASH, League.LA_LIGA, 101,
            0xFFFFFFFFL, 0xFFF7F2F2L, 0xFFEDE4E4L, 0xFFDC2626L, 0xFF111827L, 0xFFFFFFFFL, 0xFFE3C5C5L
        ),
        darkClub(
            "esp_get", "Getafe", "Águia • Azulones", ClubMascotType.AGUIA,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.LA_LIGA, 2922,
            0xFF050F26L, 0xFF0C265EL, 0xFF133B8FL, 0xFF3B82F6L, 0xFFFFFFFFL, 0xFF0B1C42L, 0xFF234DB0L
        ),
        darkClub(
            "esp_ala", "Alavés", "Babazorro • Raposa Basca", ClubMascotType.RAPOSA,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 96,
            0xFF051126L, 0xFF0C295CL, 0xFF13408CL, 0xFF38BDF8L, 0xFFFFFFFFL, 0xFF0B1E40L, 0xFF2454A8L
        ),
        lightClub(
            "esp_lpa", "Las Palmas", "Canário • Pío-Pío", ClubMascotType.CANARIO,
            ClubBackgroundPattern.PALM_WAVES, League.LA_LIGA, 98,
            0xFFFFFCE8L, 0xFFFFF4B8L, 0xFFFCE988L, 0xFF0284C7L, 0xFFD97706L, 0xFFFFFFFFL, 0xFFE5CC63L
        ),
        lightClub(
            "esp_esp", "Espanyol", "Periquito • Pericos", ClubMascotType.PERIQUITO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 88,
            0xFFF7FBFFL, 0xFFE8F3FFL, 0xFFD9EAFFL, 0xFF0284C7L, 0xFF1E3A8AL, 0xFFFFFFFFL, 0xFFB8D7F7L
        ),
        darkClub(
            "esp_leg", "Leganés", "Super Pepino • Pepineros", ClubMascotType.PEPINO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 17534,
            0xFF051324L, 0xFF0C2E54L, 0xFF144780L, 0xFF38BDF8L, 0xFF22C55EL, 0xFF0B213BL, 0xFF255B99L
        ),
        darkClub(
            "esp_vad", "Valladolid", "Coroa • Violeta Pucela", ClubMascotType.COROA_REAL,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.LA_LIGA, 95,
            0xFF140724L, 0xFF301057L, 0xFF491982L, 0xFFC084FCL, 0xFFFFFFFFL, 0xFF1F0C38L, 0xFF5D2A9CL
        ),

        // ================= UEFA CHAMPIONS LEAGUE (8 clubes icônicos) =================
        darkClub(
            "ucl_bay", "Bayern de Munique", "Leão da Baviera • Rekordmeister", ClubMascotType.LEAO_TRICOLOR,
            ClubBackgroundPattern.DIAMOND_CHECKER, League.CHAMPIONS_LEAGUE, 132,
            0xFF1A0408L, 0xFF470B16L, 0xFF6B1020L, 0xFFFF2E4CL, 0xFFFFFFFFL, 0xFF29090FL, 0xFF7D1B2CL
        ),
        darkClub(
            "ucl_psg", "PSG", "Torre Eiffel • Les Parisiens", ClubMascotType.TORRE,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.CHAMPIONS_LEAGUE, 160,
            0xFF050C1FL, 0xFF0D1E4AL, 0xFF162F70L, 0xFF38BDF8L, 0xFFEF4444L, 0xFF0B1636L, 0xFF284896L
        ),
        darkClub(
            "ucl_int", "Inter de Milão", "Biscione • Nerazzurri", ClubMascotType.DRAGAO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.CHAMPIONS_LEAGUE, 110,
            0xFF040B1CL, 0xFF091E4AL, 0xFF0E2F75L, 0xFF38BDF8L, 0xFFFFD600L, 0xFF091736L, 0xFF1E48A3L
        ),
        darkClub(
            "ucl_mil", "Milan", "Il Diavolo • Rossoneri", ClubMascotType.DIABO_VERMELHO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.CHAMPIONS_LEAGUE, 103,
            0xFF140405L, 0xFF3B090CL, 0xFF590D12L, 0xFFEF4444L, 0xFFFFFFFFL, 0xFF21080AL, 0xFF6B1A20L
        ),
        darkClub(
            "ucl_bvb", "Borussia Dortmund", "Abelha • BVB", ClubMascotType.ABELHA,
            ClubBackgroundPattern.HORIZONTAL_HOOPS, League.CHAMPIONS_LEAGUE, 124,
            0xFF141103L, 0xFF362E07L, 0xFF524609L, 0xFFFACC15L, 0xFFFFFFFFL, 0xFF211C06L, 0xFF665712L
        ),
        darkClub(
            "ucl_juv", "Juventus", "Zebra • Bianconeri", ClubMascotType.CAVALO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.CHAMPIONS_LEAGUE, 111,
            0xFF060608L, 0xFF17181FL, 0xFF272A36L, 0xFFF8FAFCL, 0xFFFFD600L, 0xFF13151BL, 0xFF3F4454L
        ),
        darkClub(
            "ucl_ben", "Benfica", "Águia Vitória • Encarnados", ClubMascotType.AGUIA,
            ClubBackgroundPattern.STADIUM_LIGHTS, League.CHAMPIONS_LEAGUE, 1929,
            0xFF1A0406L, 0xFF450A10L, 0xFF660F17L, 0xFFFF3B47L, 0xFFFFD600L, 0xFF26090CL, 0xFF751B24L
        ),
        darkClub(
            "ucl_por", "Porto", "Dragão • Azuis e Brancos", ClubMascotType.DRAGAO,
            ClubBackgroundPattern.VERTICAL_STRIPES, League.CHAMPIONS_LEAGUE, 437,
            0xFF041026L, 0xFF0B275CL, 0xFF123E8FL, 0xFF60A5FAL, 0xFFFFFFFFL, 0xFF0A1D42L, 0xFF2454B0L
        )
    )

    fun findById(id: String?): AppThemePreset {
        return allThemes.firstOrNull { it.id == id } ?: DefaultAppTheme
    }
}
