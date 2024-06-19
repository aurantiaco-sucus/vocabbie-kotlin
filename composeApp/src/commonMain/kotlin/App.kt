import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import xyz.midnight233.vocabbie.resources.CascadiaCodeNF
import xyz.midnight233.vocabbie.resources.MiSans_Regular
import xyz.midnight233.vocabbie.resources.Res

enum class Page {
    Greeting,
    StandardGameplay,
    Result,
}

@Composable
fun App() {
    var currentPage by remember { mutableStateOf(Page.Greeting) }
    MaterialTheme {
        Box(
            Modifier.fillMaxSize()
        ) {
            Box(
                Modifier.padding(10.dp)
                    .size(width = 600.dp, height = 600.dp)
                    .border(4.dp, Color.Black)
                    .align(Alignment.Center)
            ) {
                Column(
                    Modifier.padding(8.dp)
                ) {
                    val titleSize by animateFloatAsState(if (currentPage == Page.Greeting) 50f else 30f)
                    val subtitleSize by animateFloatAsState(if (currentPage == Page.Greeting) 60f else 30f)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        AnimatedVisibility(currentPage == Page.StandardGameplay) {
                            Text(
                                text = " \udb80\udc4d ",
                                fontSize = titleSize.sp,
                                fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
                                modifier = Modifier.clickable {
                                    currentPage = Page.Greeting
                                }
                            )
                        }
                        Text(
                            text = "It's Vocabbie...",
                            fontSize = titleSize.sp,
                            fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
                        )
                    }
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        }
                    ) {
                        Text(
                            text = when(it) {
                                Page.Greeting -> "Your simple\n\n\nvocabulary quiz."
                                Page.StandardGameplay -> "   What does it mean?"
                                Page.Result -> "See your feat!"
                            },
                            fontSize = subtitleSize.sp,
                            fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
                            color = MaterialTheme.colors.secondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Crossfade(
                        targetState = currentPage,
                        animationSpec = spring()
                    ) {
                        when (it) {
                            Page.Greeting -> GreetingPage(toGameplay = { currentPage = Page.StandardGameplay })
                            Page.StandardGameplay -> StandardGameplayPage(toResult = { currentPage = Page.Result })
                            Page.Result -> ResultPage()
                        }
                    }
                }
                val aboutFontSize by animateFloatAsState(if (currentPage == Page.Greeting) 30f else 20f)
                Box(
                    Modifier.align(Alignment.BottomStart)
                ) {
                    Surface(
                        color = Color.Black
                    ) {
                        Box(
                            Modifier.padding(vertical = 10.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "...created by Midnight233",
                                fontSize = aboutFontSize.sp,
                                color = Color.White,
                                fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GreetingPage(toGameplay: () -> Unit) {
    Column {
        Text(
            text = "Standard quiz \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                toGameplay()
            }
        )
    }
}

@Composable
fun StandardGameplayPage(toResult: () -> Unit) {
    Column(
        Modifier.padding(start = 20.dp)
    ) {
        Text(
            text = "mörön",
            fontSize = 80.sp,
            fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
        )
        listOf(
            "A.选项",
            "B.选项",
            "C.选项",
            "D.选项",
        ).forEach {
            Text(
                text = it,
                fontSize = 30.sp,
                fontFamily = FontFamily(Font(Res.font.MiSans_Regular), Font(Res.font.CascadiaCodeNF), ),
                modifier = Modifier.clickable {}
            )
            Spacer(Modifier.height(10.dp))
        }
        Text(
            text = "Enough! Show me the results \udb80\udc54",
            fontSize = 30.sp,
            fontFamily = FontFamily(Font(Res.font.CascadiaCodeNF)),
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                toResult()
            }
        )
    }
}

@Composable
fun ResultPage() {
    Column {

    }
}