package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainQuotesScreen()
            }
        }
    }
}

// Utility: Copy to Clipboard
fun copyQuoteToClipboard(context: Context, quote: Quote) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val formattedText = "\"${quote.text}\"\n— ${quote.author}\n\nShared via Daily World Quotes 🌍"
    val clip = ClipData.newPlainText("DailyQuote", formattedText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Quote copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
}

// Utility: Intelligent Sharing Intent with package checks and standard fallback options
fun shareQuoteWithApp(context: Context, quote: Quote, targetPackage: String?) {
    val shareBody = "✨ *Quote of the Day* ✨\n\n" +
            "\"${quote.text}\"\n\n" +
            "— *${quote.author}*\n\n" +
            "🌍 *Daily World Quotes*"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareBody)
    }

    if (targetPackage != null) {
        intent.setPackage(targetPackage)
        try {
            // Verify and lunch activity
            context.startActivity(intent)
        } catch (e: Exception) {
            val appLabel = if (targetPackage.contains("whatsapp")) "WhatsApp" else "Instagram"
            Toast.makeText(
                context,
                "$appLabel is not installed. Opening global share menu instead.",
                Toast.LENGTH_SHORT
            ).show()
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }, "Share Quote via")
            context.startActivity(chooser)
        }
    } else {
        val chooser = Intent.createChooser(intent, "Share Quote via")
        context.startActivity(chooser)
    }
}

// Persistent Favorites Loader/Saver helper
fun getLocalFavorites(context: Context): Set<Int> {
    val prefs = context.getSharedPreferences("quotes_app_prefs", Context.MODE_PRIVATE)
    val stringSet = prefs.getStringSet("favs", emptySet()) ?: emptySet()
    return stringSet.mapNotNull { it.toIntOrNull() }.toSet()
}

fun saveLocalFavorites(context: Context, favorites: Set<Int>) {
    val prefs = context.getSharedPreferences("quotes_app_prefs", Context.MODE_PRIVATE)
    prefs.edit().putStringSet("favs", favorites.map { it.toString() }.toSet()).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainQuotesScreen() {
    val context = LocalContext.current
    
    // Core functional states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var favoritesState by remember { mutableStateOf(getLocalFavorites(context)) }
    var isOnlyFavoritesSelected by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }

    // Consistent Quote of the Day
    val quoteOfTheDay = remember {
        val dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % QuoteRepository.quotes.size
        QuoteRepository.quotes[dayIndex]
    }

    // Dynamic Categories derivation
    val allCategories = remember {
        listOf("All") + QuoteRepository.quotes.map { it.category }.distinct().sorted()
    }

    // Interactive Filtering Logic
    val filteredQuotes = remember(searchQuery, selectedCategory, favoritesState, isOnlyFavoritesSelected) {
        QuoteRepository.quotes.filter { quote ->
            val matchesCategory = selectedCategory == "All" || quote.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = quote.text.contains(searchQuery, ignoreCase = true) || quote.author.contains(searchQuery, ignoreCase = true)
            val matchesFavorite = !isOnlyFavoritesSelected || favoritesState.contains(quote.id)
            matchesCategory && matchesSearch && matchesFavorite
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daily World",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Quotes",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                isOnlyFavoritesSelected = !isOnlyFavoritesSelected
                                val text = if (isOnlyFavoritesSelected) "Showing Saved Quotes ❤️" else "Showing All Quotes 🌌"
                                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("favorite_filter_toggle")
                        ) {
                            Icon(
                                imageVector = if (isOnlyFavoritesSelected) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle Favorites Filter",
                                tint = if (isOnlyFavoritesSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showAppInfoDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .testTag("info_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "App Info",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Layout Element 1: Premium Quote of the Day Hero Header (only shown when not filtering heavily)
            if (searchQuery.isEmpty() && selectedCategory == "All" && !isOnlyFavoritesSelected) {
                item {
                    Text(
                        text = "FEATURED OF THE DAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    
                    QuoteOfTheDayHero(
                        quote = quoteOfTheDay,
                        isLiked = favoritesState.contains(quoteOfTheDay.id),
                        onLikeChange = {
                            val nextFavs = favoritesState.toMutableSet()
                            if (nextFavs.contains(quoteOfTheDay.id)) {
                                nextFavs.remove(quoteOfTheDay.id)
                            } else {
                                nextFavs.add(quoteOfTheDay.id)
                            }
                            favoritesState = nextFavs
                            saveLocalFavorites(context, nextFavs)
                        },
                        onCopyClick = { copyQuoteToClipboard(context, quoteOfTheDay) },
                        onShareCustomApp = { pkg -> shareQuoteWithApp(context, quoteOfTheDay, pkg) }
                    )
                }
            }

            // Layout Element 2: Custom Search & Category Filters Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = { Text("Search by author, keywords...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Search",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Category Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("categories_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(allCategories) { category ->
                            val isSelected = selectedCategory == category
                            val containerColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                label = "chipBg"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                label = "chipText"
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(containerColor)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("category_chip_${category.lowercase()}")
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Layout Element 3: Header for the Quote Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOnlyFavoritesSelected) "SAVED CODES (${filteredQuotes.size})" else "EXPLORE QUOTES (${filteredQuotes.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    if (searchQuery.isNotEmpty() || selectedCategory != "All" || isOnlyFavoritesSelected) {
                        Text(
                            text = "Reset Filters",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .clickable {
                                    searchQuery = ""
                                    selectedCategory = "All"
                                    isOnlyFavoritesSelected = false
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }

            // Layout Element 4: Dynamic List Render or Empty State
            if (filteredQuotes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .testTag("empty_state_view"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🌫️",
                            fontSize = 54.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "No Quotes Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your filters, searching for alternate keywords, or saving quotes first!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                items(
                    items = filteredQuotes,
                    key = { it.id }
                ) { quote ->
                    QuoteCard(
                        quote = quote,
                        isLiked = favoritesState.contains(quote.id),
                        onLikeChange = {
                            val nextFavs = favoritesState.toMutableSet()
                            if (nextFavs.contains(quote.id)) {
                                nextFavs.remove(quote.id)
                            } else {
                                nextFavs.add(quote.id)
                            }
                            favoritesState = nextFavs
                            saveLocalFavorites(context, nextFavs)
                        },
                        onCopyClick = { copyQuoteToClipboard(context, quote) },
                        onShareCustomApp = { pkg -> shareQuoteWithApp(context, quote, pkg) }
                    )
                }
            }
        }
    }

    // Modern Elegant Dialog
    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) {
                    Text("Got It", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Daily World Quotes 🌍", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Welcome to Daily World Quotes! Your companion for clean, curated motivational thoughts from great minds around high visual fidelity.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "📱 Quick Social Share Features:",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Text(
                        "• WhatsApp Share: Formats the text perfectly with asterisks and custom templates and targets WhatsApp natively.\n" +
                        "• Instagram Share: Prefills your chosen share content into templates ready for Instagram feeds, stories, or messages natively.\n" +
                        "• Fallback Engine: If WhatsApp or Instagram aren't detected on your active device, the app intelligently and seamlessly loads the native Android system sharing tray so you never hit a dead-end!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Created with Cosmic Slate dark theme for perfect legibility.",
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.testTag("info_dialog")
        )
    }
}

@Composable
fun QuoteOfTheDayHero(
    quote: Quote,
    isLiked: Boolean,
    onLikeChange: () -> Unit,
    onCopyClick: () -> Unit,
    onShareCustomApp: (String?) -> Unit
) {
    // Elegant Multi-tone Linear Gradient Brush for Gold/Bronze Celestial highlights
    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1D1B13),
                Color(0xFF13151D)
            )
        )
    }
    
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heartScaleAnimHero"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, shape = RoundedCornerShape(24.dp), ambientColor = Color.Black)
            .border(1.dp, Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), Color.Transparent)), RoundedCornerShape(24.dp))
            .testTag("quote_of_the_day_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "💡 QUOTE OF THE DAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onLikeChange() }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save Hero Quote",
                            tint = if (isLiked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.scale(heartScale)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Beautiful quote body
                Text(
                    text = "“",
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    lineHeight = 10.sp,
                    modifier = Modifier.offset(y = 10.dp)
                )

                Text(
                    text = quote.text,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Text(
                    text = "”",
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    lineHeight = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-10).dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Author signature
                Text(
                    text = "— ${quote.author}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), thickness = 1.dp)

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Social Sharing Action Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Copy to Clipboard Action
                    TextButton(
                        onClick = onCopyClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("copy_button_hero")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy Quote To Clipboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Social Share Actions Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // WhatsApp Trigger Action
                        Button(
                            onClick = { onShareCustomApp("com.whatsapp") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("share_whatsapp_hero")
                        ) {
                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Instagram Trigger Action
                        Button(
                            onClick = { onShareCustomApp("com.instagram.android") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF833AB4),
                                            Color(0xFFFD1D1D),
                                            Color(0xFFF56040)
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .testTag("share_instagram_hero")
                        ) {
                            Text("Instagram", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Standard Generic Share
                        IconButton(
                            onClick = { onShareCustomApp(null) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .testTag("share_global_hero")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Global Share Option",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteCard(
    quote: Quote,
    isLiked: Boolean,
    onLikeChange: () -> Unit,
    onCopyClick: () -> Unit,
    onShareCustomApp: (String?) -> Unit
) {
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "heartScaleAnimQuoteCard"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(20.dp))
            .border(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("quote_card_${quote.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Category tag and Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quote.emoji,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = quote.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onLikeChange() }
                        .padding(4.dp)
                        .testTag("like_button_${quote.id}")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save Quote",
                        tint = if (isLiked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(20.dp)
                            .scale(heartScale)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text
            Text(
                text = "“${quote.text}”",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Author alignment
            Text(
                text = "— ${quote.author}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Actions: Copy, Share on Instagram, Share on WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Copy Action
                IconButton(
                    onClick = onCopyClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .testTag("copy_button_${quote.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy text",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // WhatsApp, Instagram specialized quick share icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick WhatsApp button
                    Button(
                        onClick = { onShareCustomApp("com.whatsapp") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("whatsapp_share_button_${quote.id}")
                    ) {
                        Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Quick Instagram button
                    Button(
                        onClick = { onShareCustomApp("com.instagram.android") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF833AB4),
                                        Color(0xFFFD1D1D),
                                        Color(0xFFF56040)
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .testTag("instagram_share_button_${quote.id}")
                    ) {
                        Text("Instagram", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // General Share
                    IconButton(
                        onClick = { onShareCustomApp(null) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("general_share_button_${quote.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Global share menu",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
