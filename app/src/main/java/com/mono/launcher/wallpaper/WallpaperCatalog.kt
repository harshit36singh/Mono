package com.mono.launcher.wallpaper

import com.mono.launcher.R

/** Wallpapers bundled with Mono (generated from the design assets). */
object WallpaperCatalog {
    val all: List<Wallpaper> = listOf(
        Wallpaper("wp_black_marble_with_white_veins", "Black Marble with White Veins", R.drawable.wp_black_marble_with_white_veins, R.drawable.wp_black_marble_with_white_veins_thumb),
        Wallpaper("wp_crumpled_paper_edge_in_monochrome", "Crumpled Paper Edge in Monochrome", R.drawable.wp_crumpled_paper_edge_in_monochrome, R.drawable.wp_crumpled_paper_edge_in_monochrome_thumb),
        Wallpaper("wp_dramatic_grayscale_icefall_explosion", "Dramatic Grayscale Icefall Explosion", R.drawable.wp_dramatic_grayscale_icefall_explosion, R.drawable.wp_dramatic_grayscale_icefall_explosion_thumb),
        Wallpaper("wp_intricate_leaf_skeleton_on_black", "Intricate Leaf Skeleton on Black", R.drawable.wp_intricate_leaf_skeleton_on_black, R.drawable.wp_intricate_leaf_skeleton_on_black_thumb),
        Wallpaper("wp_luminous_s_curve_over_textured_darkness", "Luminous S-Curve Over Textured Darkness", R.drawable.wp_luminous_s_curve_over_textured_darkness, R.drawable.wp_luminous_s_curve_over_textured_darkness_thumb),
        Wallpaper("wp_monochrome_blossoming_tree_against_darkness", "Monochrome Blossoming Tree Against Darkness", R.drawable.wp_monochrome_blossoming_tree_against_darkness, R.drawable.wp_monochrome_blossoming_tree_against_darkness_thumb),
        Wallpaper("wp_monochrome_cracked_coastline_texture", "Monochrome Cracked Coastline Texture", R.drawable.wp_monochrome_cracked_coastline_texture, R.drawable.wp_monochrome_cracked_coastline_texture_thumb),
        Wallpaper("wp_monochrome_cracked_surface_edge", "Monochrome Cracked Surface Edge", R.drawable.wp_monochrome_cracked_surface_edge, R.drawable.wp_monochrome_cracked_surface_edge_thumb),
        Wallpaper("wp_monochrome_feather_macro_on_black", "Monochrome Feather Macro on Black", R.drawable.wp_monochrome_feather_macro_on_black, R.drawable.wp_monochrome_feather_macro_on_black_thumb),
        Wallpaper("wp_monochrome_forest_light_streaks", "Monochrome Forest Light Streaks", R.drawable.wp_monochrome_forest_light_streaks, R.drawable.wp_monochrome_forest_light_streaks_thumb),
        Wallpaper("wp_monochrome_spiral_coastline_textures", "Monochrome Spiral Coastline Textures", R.drawable.wp_monochrome_spiral_coastline_textures, R.drawable.wp_monochrome_spiral_coastline_textures_thumb),
        Wallpaper("wp_monochrome_waves_along_rugged_shoreline", "Monochrome Waves Along Rugged Shoreline", R.drawable.wp_monochrome_waves_along_rugged_shoreline, R.drawable.wp_monochrome_waves_along_rugged_shoreline_thumb),
        Wallpaper("wp_moonlit_craters_along_the_terminator", "Moonlit Craters Along the Terminator", R.drawable.wp_moonlit_craters_along_the_terminator, R.drawable.wp_moonlit_craters_along_the_terminator_thumb),
        Wallpaper("wp_moonlit_japanese_neighbourhood", "Moonlit Japanese Neighbourhood", R.drawable.wp_moonlit_japanese_neighbourhood, R.drawable.wp_moonlit_japanese_neighbourhood_thumb),
        Wallpaper("wp_swirling_white_smoke_plumes", "Swirling White Smoke Plumes", R.drawable.wp_swirling_white_smoke_plumes, R.drawable.wp_swirling_white_smoke_plumes_thumb),
        Wallpaper("wp_whale_skeleton_along_the_shoreline", "Whale Skeleton Along the Shoreline", R.drawable.wp_whale_skeleton_along_the_shoreline, R.drawable.wp_whale_skeleton_along_the_shoreline_thumb),
    )

    fun byId(id: String?): Wallpaper? = all.firstOrNull { it.id == id }
}
