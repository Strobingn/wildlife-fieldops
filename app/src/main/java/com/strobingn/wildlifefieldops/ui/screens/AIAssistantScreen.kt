package com.strobingn.wildlifefieldops.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.strobingn.wildlifefieldops.ui.theme.*
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBack: () -> Unit
) {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage("Hello! I'm your Wildlife FieldOps AI assistant. I can help you with:\n\n- Species identification tips\n- Wildlife handling procedures\n- Regulatory compliance\n- Equipment recommendations\n- Safety protocols\n\nWhat would you like to know?", false)
    )) }
    var inputText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Assistant", color = TextPrimary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickChip("Species ID", Icons.Default.Pets) {
                    inputText = "How do I identify common wildlife species?"
                }
                QuickChip("Safety", Icons.Default.HealthAndSafety) {
                    inputText = "What are the safety protocols for wildlife removal?"
                }
                QuickChip("Equipment", Icons.Default.Handyman) {
                    inputText = "What equipment do I need for trapping?"
                }
            }

            // Chat Messages
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
                if (isTyping) {
                    item {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking...", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask me anything...", color = TextTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BackgroundCard,
                        unfocusedContainerColor = BackgroundCard
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) sendMessage()
                    }),
                    maxLines = 3
                )
                IconButton(
                    onClick = { if (inputText.isNotBlank()) sendMessage() },
                    enabled = inputText.isNotBlank() && !isTyping,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AccentPurple,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        disabledContainerColor = SurfaceVariant,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }

    fun sendMessage() {
        val userMessage = inputText.trim()
        messages = messages + ChatMessage(userMessage, true)
        inputText = ""
        isTyping = true

        // Simulate AI response
        coroutineScope.launch {
            kotlinx.coroutines.delay(1000 + (500..2000).random().toLong())
            val response = generateAIResponse(userMessage)
            messages = messages + ChatMessage(response, false)
            isTyping = false
        }
    }
}

private fun generateAIResponse(userMessage: String): String {
    val lowerMessage = userMessage.lowercase()
    return when {
        lowerMessage.contains("species") || lowerMessage.contains("identif") || lowerMessage.contains("raccoon") || lowerMessage.contains("squirrel") || lowerMessage.contains("skunk") ->
            "Here are key identification tips:\n\n**Raccoons**: Black mask, ringed tail, dexterous front paws. Nocturnal. Average 10-30 lbs.\n\n**Gray Squirrels**: Bushy tail, gray/brown fur. Active dawn/dusk. Look for chewed entry points.\n\n**Skunks**: Black with white stripes/spots. Warning: Can spray 10-15 feet! Approach with caution.\n\n**Bats**: Small, winged mammals. Often found in attics/belfries. Protected species in many areas - check regulations before removal.\n\nWould you like specific details about any species?"

        lowerMessage.contains("safety") || lowerMessage.contains("protocol") || lowerMessage.contains("protect") ->
            "**Wildlife Removal Safety Protocols:**\n\n1. **PPE Required**: Thick gloves, eye protection, long sleeves, respirator for attics\n2. **Rabies Vector Species**: Raccoons, bats, skunks, foxes - minimize direct contact\n3. **Ladder Safety**: Always have a spotter for elevated entries\n4. **Containment**: Use secure transfer cages with solid dividers\n5. **Documentation**: Photo all entry points before and after\n6. **Release Protocol**: Check local regulations for relocation distances (typically 10+ miles)\n\nAlways have your rabies pre-exposure vaccination current!"

        lowerMessage.contains("equipment") || lowerMessage.contains("tool") || lowerMessage.contains("trap") ->
            "**Essential Wildlife Control Equipment:**\n\n**Trapping**: Live cage traps (32\" raccoon, 24\" squirrel, 10\" rat), trap dividers, bait (sardines, peanut butter, marshmallows)\n\n**Exclusion**: Heavy-gauge wire mesh (1/4\" or 1/2\"), chimney caps, vent covers, foam sealant\n\n**Safety**: Kevlar gloves, respirator (N95 minimum), headlamp, snake tongs\n\n**Inspection**: Borescope/camera for void spaces, moisture meter, UV light for droppings\n\n**Documentation**: Camera with flash, measuring tape, GPS unit\n\nDo you need recommendations for a specific situation?"

        lowerMessage.contains("regulation") || lowerMessage.contains("legal") || lowerMessage.contains("permit") ->
            "**Important Regulatory Considerations:**\n\n- **Federal**: Migratory Bird Treaty Act protects most birds. Endangered Species Act may apply.\n- **State**: Wildlife removal permits often required. Check your state DNR requirements.\n- **Local**: Some municipalities have specific ordinances about trapping and relocation.\n- **Protected Species**: Bats (during maternity season), migratory birds, and some turtles require special handling.\n- **Relocation**: Many states require animals be relocated on the same property or within the county.\n\nAlways verify current regulations with your state wildlife agency before proceeding."

        lowerMessage.contains("price") || lowerMessage.contains("cost") || lowerMessage.contains("charge") || lowerMessage.contains("estimate") ->
            "**Typical Wildlife Service Pricing:**\n\n**Inspection**: $150-$300 (often credited toward work)\n**Squirrel Removal**: $300-$600 (includes entry sealing)\n**Raccoon Removal**: $400-$800 (varies by location complexity)\n**Bat Exclusion**: $500-$2,000+ (depends on colony size/structure)\n**Bird Control**: $200-$1,500 (netting, spikes, exclusion)\n**Dead Animal**: $200-$500 (location dependent)\n\nFactors affecting price: accessibility, number of entry points, repairs needed, warranty length. Most companies offer 1-year warranties on exclusion work."

        else ->
            "That's a great question about wildlife operations. Based on my knowledge:\n\nFor wildlife control work, I always recommend following these core principles:\n\n1. **Humane treatment** - Use live traps when possible\n2. **Complete exclusion** - Seal all entry points to prevent re-entry\n3. **Habitat modification** - Remove attractants (food sources, shelter)\n4. **Documentation** - Thorough records for warranty and compliance\n5. **Customer education** - Teach prevention strategies\n\nCould you provide more details about your specific situation? I can give more targeted advice about species identification, trapping strategies, or exclusion techniques."
    }
}

@Composable
private fun QuickChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AccentPurple.copy(alpha = 0.15f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = AccentPurple)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val backgroundColor = if (message.isUser) AccentPurple.copy(alpha = 0.2f) else BackgroundElevated
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val textColor = if (message.isUser) TextPrimary else TextPrimary

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.padding(horizontal = if (message.isUser) 32.dp else 0.dp)
        ) {
            if (!message.isUser) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Assistant", style = MaterialTheme.typography.labelSmall, color = AccentPurple, fontWeight = FontWeight.Medium)
                }
            }
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
