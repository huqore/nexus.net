package net.nexus.app.ui

/**
 * Authentic vector coastlines and political borders for 3D Globe rendering.
 * Stored as pairs of [latitude, longitude] in degrees.
 */
object WorldVectors {

    val EUROPE_COAST = floatArrayOf(
        // Iberia
        36.0f, -5.6f, 37.0f, -9.0f, 38.8f, -9.5f, 42.0f, -8.9f, 43.5f, -9.3f, 43.6f, -7.5f,
        // France Biscay
        43.4f, -1.8f, 46.0f, -1.2f, 47.3f, -3.0f, 48.4f, -4.8f, 49.7f, -1.9f, 49.5f, 0.1f,
        // Low Countries (Belgium, Netherlands)
        51.0f, 2.5f, 51.4f, 3.4f, 52.0f, 4.1f, 52.5f, 4.6f, 53.2f, 5.4f, 53.5f, 6.9f,
        // Germany & Denmark
        53.9f, 8.6f, 55.0f, 8.5f, 57.1f, 8.6f, 57.7f, 10.6f, 55.5f, 10.0f, 54.5f, 11.0f,
        // Baltic Germany & Poland
        54.2f, 12.1f, 54.4f, 13.6f, 53.9f, 14.3f, 54.3f, 16.2f, 54.6f, 18.6f, 54.4f, 19.8f,
        // Baltic States
        55.7f, 21.1f, 57.6f, 22.6f, 56.9f, 24.1f, 58.4f, 24.5f, 59.4f, 24.8f, 59.5f, 28.0f,
        // Finland Gulf & Coast
        60.2f, 25.0f, 60.5f, 27.2f, 60.6f, 28.7f, 60.4f, 22.2f, 61.5f, 21.5f, 63.1f, 21.5f, 65.0f, 25.4f, 65.8f, 24.1f,
        // Sweden
        65.6f, 22.2f, 63.8f, 20.3f, 62.4f, 17.3f, 60.6f, 17.2f, 59.3f, 18.1f, 58.6f, 16.2f,
        56.7f, 16.4f, 55.9f, 14.3f, 55.4f, 12.8f, 56.0f, 12.6f, 57.7f, 11.9f, 58.9f, 11.1f,
        // Norway
        59.0f, 10.5f, 58.0f, 8.0f, 58.1f, 6.6f, 60.4f, 5.0f, 62.5f, 6.1f, 64.0f, 10.0f,
        67.3f, 14.4f, 69.7f, 19.0f, 71.2f, 25.8f, 70.4f, 31.0f, 69.0f, 33.0f,
        // Russia North & White Sea
        67.0f, 41.0f, 64.5f, 40.5f, 65.5f, 34.5f, 68.0f, 50.0f, 69.0f, 60.0f, 73.0f, 70.0f,
        // Black Sea & Caspian
        46.0f, 48.0f, 41.0f, 50.0f, 37.0f, 50.0f,
        46.5f, 30.7f, 44.2f, 28.6f, 42.0f, 28.0f, 41.0f, 29.0f,
        // Greece & Balkans
        40.5f, 23.0f, 38.0f, 23.8f, 36.5f, 22.5f, 38.5f, 21.0f, 39.5f, 20.0f, 42.0f, 19.0f,
        // Italy & Adriatic
        45.5f, 13.6f, 45.4f, 12.3f, 44.0f, 12.5f, 41.9f, 15.0f, 40.4f, 18.0f, 38.0f, 15.7f,
        38.2f, 13.3f, 40.8f, 14.3f, 41.9f, 12.5f, 43.7f, 10.3f, 44.4f, 8.9f,
        // South France & Spain Mediterranean
        43.7f, 7.3f, 43.3f, 5.3f, 43.5f, 3.5f, 42.4f, 3.2f, 41.4f, 2.2f, 36.0f, -5.6f
    )

    val UK_COAST = floatArrayOf(
        50.0f, -5.7f, 50.6f, -2.4f, 51.1f, 1.4f, 51.9f, 1.3f, 52.9f, 0.5f,
        53.6f, 0.1f, 54.5f, -0.6f, 55.8f, -1.9f, 57.5f, -1.8f, 58.6f, -3.1f,
        58.5f, -5.0f, 56.8f, -6.1f, 55.4f, -5.0f, 54.6f, -3.6f, 53.3f, -4.5f,
        51.6f, -5.1f, 51.2f, -3.2f, 50.2f, -3.8f, 50.0f, -5.7f
    )

    val IRELAND_COAST = floatArrayOf(
        51.5f, -9.9f, 51.7f, -8.5f, 52.2f, -6.3f, 53.3f, -6.0f, 54.0f, -6.2f,
        55.2f, -6.8f, 55.2f, -7.5f, 54.5f, -8.7f, 53.5f, -10.0f, 52.6f, -9.9f, 51.5f, -9.9f
    )

    val AFRICA_COAST = floatArrayOf(
        36.0f, -5.3f, 35.8f, -0.8f, 37.0f, 10.0f, 33.0f, 11.5f, 31.0f, 20.0f, 31.5f, 25.0f,
        31.3f, 31.5f, 30.0f, 32.5f, 27.5f, 34.0f, 22.0f, 37.0f, 12.0f, 43.5f, 11.5f, 51.0f,
        2.0f, 45.0f, -4.5f, 39.5f, -11.0f, 40.5f, -17.0f, 39.0f, -26.0f, 33.0f, -34.0f, 26.0f,
        -34.8f, 20.0f, -34.0f, 18.4f, -28.5f, 16.5f, -22.5f, 14.5f, -12.5f, 13.5f, -5.0f, 12.0f,
        4.0f, 9.0f, 4.5f, 5.0f, 5.5f, 0.0f, 5.0f, -4.0f, 4.5f, -7.5f, 6.0f, -11.0f,
        9.5f, -13.5f, 12.0f, -16.5f, 14.5f, -17.5f, 16.0f, -16.5f, 21.0f, -17.0f,
        28.0f, -13.0f, 31.5f, -10.0f, 34.0f, -6.8f, 36.0f, -5.3f
    )

    val ASIA_COAST = floatArrayOf(
        31.3f, 34.3f, 28.0f, 34.5f, 22.0f, 38.0f, 12.5f, 44.0f, 14.0f, 50.0f, 22.0f, 60.0f,
        25.0f, 56.5f, 27.0f, 50.0f, 30.0f, 48.5f, 29.0f, 50.5f, 27.0f, 56.0f, 25.0f, 62.0f,
        24.0f, 68.0f, 20.0f, 73.0f, 10.0f, 76.0f, 8.0f, 77.5f, 13.0f, 80.0f, 20.0f, 86.0f,
        22.0f, 91.0f, 16.0f, 94.0f, 8.0f, 98.0f, 1.3f, 103.8f, 8.0f, 105.0f, 14.0f, 109.0f,
        21.5f, 108.5f, 23.0f, 116.5f, 30.0f, 122.0f, 36.0f, 120.0f, 39.0f, 124.0f,
        40.0f, 128.0f, 43.0f, 132.0f, 48.0f, 140.0f, 55.0f, 155.0f, 60.0f, 166.0f
    )

    // European Country National Borders
    val BORDERS: List<FloatArray> = listOf(
        // Germany - Netherlands & Belgium
        floatArrayOf(53.5f, 6.9f, 53.2f, 7.2f, 52.2f, 7.0f, 51.8f, 6.0f, 51.3f, 6.0f, 50.8f, 6.0f, 50.2f, 6.1f),
        // Germany - France
        floatArrayOf(50.2f, 6.1f, 49.3f, 6.6f, 49.0f, 8.2f, 47.6f, 7.6f),
        // Germany - Switzerland & Austria (South Border)
        floatArrayOf(47.6f, 7.6f, 47.7f, 9.0f, 47.5f, 9.7f, 47.5f, 11.0f, 47.5f, 12.5f, 47.7f, 13.0f, 48.6f, 13.8f),
        // Germany - Czechia
        floatArrayOf(48.6f, 13.8f, 49.3f, 12.5f, 50.3f, 12.1f, 50.8f, 14.2f, 50.9f, 14.9f),
        // Germany - Poland (Oder-Neisse Line)
        floatArrayOf(50.9f, 14.9f, 51.5f, 15.0f, 52.2f, 14.6f, 52.8f, 14.3f, 53.6f, 14.4f, 53.9f, 14.3f),
        // Poland - Czechia & Slovakia (South Border)
        floatArrayOf(50.9f, 14.9f, 50.4f, 16.3f, 50.0f, 17.7f, 49.8f, 18.5f, 49.5f, 19.5f, 49.3f, 20.8f, 49.1f, 22.5f),
        // Poland - Ukraine, Belarus & Lithuania (East Border)
        floatArrayOf(49.1f, 22.5f, 50.0f, 23.5f, 50.8f, 24.1f, 52.0f, 23.2f, 53.0f, 23.8f, 53.9f, 23.5f, 54.3f, 22.8f, 54.4f, 19.8f),
        // Sweden - Norway
        floatArrayOf(59.0f, 11.8f, 60.0f, 12.5f, 61.5f, 12.5f, 63.0f, 12.1f, 64.5f, 13.5f, 66.0f, 15.0f, 68.0f, 18.0f, 68.5f, 20.5f),
        // Finland - Sweden (Torne River)
        floatArrayOf(68.5f, 20.5f, 67.5f, 23.5f, 65.8f, 24.1f),
        // Finland - Norway & Russia (East Border)
        floatArrayOf(68.5f, 20.5f, 69.8f, 26.0f, 69.0f, 29.0f, 66.5f, 29.8f, 64.5f, 30.2f, 62.8f, 31.5f, 61.0f, 28.8f, 60.6f, 28.7f)
    )

    val ALL_COASTLINES: List<FloatArray> = listOf(
        EUROPE_COAST,
        UK_COAST,
        IRELAND_COAST,
        AFRICA_COAST,
        ASIA_COAST
    )
}

