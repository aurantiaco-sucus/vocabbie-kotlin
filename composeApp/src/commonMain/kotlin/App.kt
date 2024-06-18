import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class Page {
    Greeting,
    Gameplay,
}

@Composable
fun App() {
    var currentPage by remember { mutableStateOf(Page.Greeting) }
    MaterialTheme {
        Box(
            Modifier.fillMaxSize()
        ) {
            Column(
                Modifier.padding(10.dp)
            ) {
                Row {
                    AnimatedVisibility(currentPage == Page.Gameplay) {
                        TextButton(
                            onClick = {
                                currentPage = Page.Greeting
                            }
                        ) {
                            Text(
                                text = "↼",
                                fontSize = 50.sp
                            )
                        }
                    }
                    Text(
                        text = "It's Vocabbie...",
                        fontSize = 50.sp,
                    )
                }
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    }
                ) {
                    when (it) {
                        Page.Greeting -> Text(
                            text = "The vocabulary quiz!",
                            fontSize = 50.sp,
                        )
                        Page.Gameplay -> Text(
                            text = "Bring it on now!",
                            fontSize = 50.sp,
                        )
                    }

                }
                Crossfade(currentPage) {
                    when (it) {
                        Page.Greeting -> GreetingPage(toGameplay = { currentPage = Page.Gameplay })
                        Page.Gameplay -> GameplayPage()
                    }
                }
            }
            Box(
                Modifier.align(Alignment.BottomStart)
            ) {
                Surface(
                    color = Color.Black
                ) {
                    Box(
                        Modifier.padding(10.dp)
                    ) {
                        Text(
                            text = "...created by Midnight233",
                            fontSize = 40.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingPage(toGameplay: () -> Unit) {
    Button(
        onClick = toGameplay
    ) {}
}

@Composable
fun GameplayPage() {

}