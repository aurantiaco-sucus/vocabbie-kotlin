import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.Font
import xyz.midnight233.vocabbie.resources.CascadiaCodeNF
import xyz.midnight233.vocabbie.resources.MiSans_Regular
import xyz.midnight233.vocabbie.resources.Res

val targetHost = when(getPlatform()::class.simpleName) {
    "AndroidPlatform" -> "10.0.2.2:8000"
    else -> "localhost:8000"
}

lateinit var appSans: FontFamily
lateinit var appMono: FontFamily

enum class Page {
    Greeting,
    StandardGameplay,
    RecallGameplay,
    Result,
}

lateinit var currentPageState: MutableState<Page>

lateinit var sessionIdState: MutableState<String>

data class StandardGameplayState(
    val result_available: Boolean,
    val question: String,
    val candidates: List<String>,
    val answer: Int?,
    var choice: Int? = null,
    var is_correct: Boolean? = null,
)

lateinit var standardGameplayState: MutableState<StandardGameplayState>

data class Results(
    val uls: Int,
    val rfwls: Int
)

lateinit var resultsState: MutableState<Results>

data class RecallGameplayState(
    val result_available: Boolean,
    val question: String,
)

lateinit var recallGameplayState: MutableState<RecallGameplayState>

val httpClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

@Composable
fun App() {
    appSans = FontFamily(Font(Res.font.MiSans_Regular))
    appMono = FontFamily(Font(Res.font.CascadiaCodeNF))
    currentPageState = remember { mutableStateOf(Page.Greeting) }
    sessionIdState = remember { mutableStateOf("0") }
    standardGameplayState = remember { mutableStateOf(StandardGameplayState(
        false, "", listOf(), 0)) }
    resultsState = remember { mutableStateOf(Results(0, 0)) }
    recallGameplayState = remember { mutableStateOf(RecallGameplayState(false, "")) }

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
                RootPage()
                CreditBar()
            }
        }
    }
}

@Composable
fun RootPage() {
    var currentPage by currentPageState
    Column(
        Modifier.padding(8.dp)
    ) {
        val titleSize by animateFloatAsState(when(currentPage) {
            Page.Greeting -> 50f
            Page.StandardGameplay -> 30f
            Page.RecallGameplay -> 40f
            Page.Result -> 30f
        })
        val subtitleSize by animateFloatAsState(
            when(currentPage) {
                Page.Greeting -> 60f
                Page.StandardGameplay -> 30f
                Page.RecallGameplay -> 40f
                Page.Result -> 30f
            }
        )
        Spacer(Modifier.height(10.dp))
        Row {
            AnimatedVisibility(currentPage != Page.Greeting) {
                Text(
                    text = " \udb80\udc4d ",
                    fontSize = titleSize.sp,
                    fontFamily = appMono,
                    modifier = Modifier.clickable {
                        currentPage = Page.Greeting
                    }
                )
            }
            Text(
                text = "It's Vocabbie...",
                fontSize = titleSize.sp,
                fontFamily = appMono,
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
                text = when (it) {
                    Page.Greeting -> "Your simple\n\n\nvocabulary quiz."
                    Page.StandardGameplay -> "   What does it mean?"
                    Page.Result -> "   See your feat!"
                    Page.RecallGameplay -> "   What's your word?"
                },
                fontSize = subtitleSize.sp,
                fontFamily = appMono,
                color = MaterialTheme.colors.secondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Crossfade(
            targetState = currentPage,
            animationSpec = spring()
        ) {
            when (it) {
                Page.Greeting -> GreetingPage()
                Page.StandardGameplay -> StandardGameplayPage()
                Page.Result -> ResultPage()
                Page.RecallGameplay -> RecallGameplayPage()
            }
        }
    }
}

@Composable
fun BoxScope.CreditBar() {
    val currentPage by currentPageState
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
                    fontFamily = appMono,
                )
            }
        }
    }
}

@Serializable
data class Message(
    val session: Long,
    val details: HashMap<String, String>,
)

@Composable
fun GreetingPage() {
    val coroutineScope = rememberCoroutineScope()
    Column {
        Text(
            text = "Standard quiz \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchStandardSession()
                }
            }
        )
        Text(
            text = "Recall test \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchRecallSession()
                }
            }
        )
    }
}

suspend fun launchStandardSession() {
    var currentPage by currentPageState
    var sessionId by sessionIdState
    var standardGameplayState by standardGameplayState
    val rStart = httpClient.post("http://$targetHost/start") {
        contentType(ContentType.Application.Json)
        setBody(Message(0, hashMapOf("kind" to "standard")))
    }.body<Message>()
    if (rStart.session == 0L) {
        println("Failed to start a session.")
        return
    }
    println("New standard session ID is ${rStart.session}")
    sessionId = rStart.session.toString()
    val rState = httpClient.post("http://$targetHost/state") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf()))
    }.body<Message>()
    val state = StandardGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        question = rState.details["question"]!!,
        candidates = rState.details["candidates"]!!.split(";;;"),
        answer = rState.details["answer"]?.toInt()
    )
    standardGameplayState = state
    currentPage = Page.StandardGameplay
}

suspend fun launchRecallSession() {
    var currentPage by currentPageState
    var sessionId by sessionIdState
    var recallGameplayState by recallGameplayState
    val rStart = httpClient.post("http://$targetHost/start") {
        contentType(ContentType.Application.Json)
        setBody(Message(0, hashMapOf("kind" to "recall")))
    }.body<Message>()
    if (rStart.session == 0L) {
        println("Failed to start a session.")
        return
    }
    println("New standard session ID is ${rStart.session}")
    sessionId = rStart.session.toString()
    val rState = httpClient.post("http://$targetHost/state") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf()))
    }.body<Message>()
    val state = RecallGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        question = rState.details["question"]!!
    )
    recallGameplayState = state
    currentPage = Page.RecallGameplay
}

@Composable
fun StandardGameplayPage() {
    val standardGameplayState by standardGameplayState
    val choiceTintTargetState: MutableState<Pair<Int, Boolean>?> = remember { mutableStateOf(null) }
    val choiceTintTarget by choiceTintTargetState
    val questionFontSize by animateFloatAsState(when (standardGameplayState.question.length) {
        in 0..10 -> 70f
        in 11..15 -> 60f
        in 16..20 -> 50f
        in 21..25 -> 40f
        else -> 30f
    })
    val coroutineScope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = standardGameplayState,
            transitionSpec = { (slideInHorizontally { x -> x } + fadeIn())
                .togetherWith(slideOutHorizontally { x -> -x } + fadeOut()) }
        ) { state -> Column(Modifier.padding(start = 20.dp)) {
            Spacer(Modifier.height(70.dp - questionFontSize.dp))
            Text(
                text = state.question,
                fontSize = questionFontSize.sp,
                lineHeight = (questionFontSize + 5).sp,
                fontFamily = appSans,
            )
            Spacer(Modifier.height(10.dp))
            state.candidates.fastForEachIndexed { i, it ->
                val tint by animateColorAsState(when(choiceTintTarget) {
                    i to true -> Color.Green
                    i to false -> Color.Red
                    else -> Color.DarkGray
                })
                Text(
                    text = "$it ${if (state.answer == i) "✦" else ""}",
                    fontSize = if (it.length < 15) 35.sp else 25.sp,
                    lineHeight = 40.sp,
                    fontFamily = appSans,
                    color = tint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(25))
                        .clickable {
                            if (state.choice == null) {
                                standardGameplayState.choice = i
                                coroutineScope.launch {
                                    updateStandardSession(choiceTintTargetState)
                                }
                            }
                        }
                )
                Spacer(Modifier.height(10.dp))
            }
        } }
        AnimatedVisibility (
            visible = standardGameplayState.result_available,
            enter = fadeIn(),
        ) {
            Text(
                text = "Enough! Show me the results \udb80\udc54",
                fontSize = 30.sp,
                fontFamily = appMono,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(start = 20.dp).clickable { coroutineScope.launch {
                    finishSession()
                } }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

suspend fun updateStandardSession(tintState: MutableState<Pair<Int, Boolean>?>) {
    val sessionId by sessionIdState
    var state by standardGameplayState
    var tint by tintState
    val rSubmit = httpClient.post("http://$targetHost/submit") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf(
            "action" to "choose",
            "choice" to state.choice!!.toString()
        )))
    }.body<Message>()
    val correct = rSubmit.details["correct"]!!.toBoolean()
    if (correct) {
        tint = Pair(state.choice!!, true)
    } else {
        tint = Pair(state.choice!!, false)
    }
    delay(500)
    tint = null
    delay(250)
    val rState = httpClient.post("http://$targetHost/state") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf()))
    }.body<Message>()
    state = StandardGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        question = rState.details["question"]!!,
        candidates = rState.details["candidates"]!!.split(";;;"),
        answer = rState.details["answer"]?.toInt()
    )
}

suspend fun finishSession() {
    var currentPage by currentPageState
    val sessionId by sessionIdState
    var results by resultsState
    val rFinish = httpClient.post("http://$targetHost/submit") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf("action" to "finish")))
    }.body<Message>()
    println("Session $sessionId finished.")
    results = Results(
        rFinish.details["uls"]!!.toInt(),
        rFinish.details["rfwls"]!!.toInt()
    )
    currentPage = Page.Result
}

@Composable
fun ResultPage() {
    val results by resultsState
    Column(
        Modifier.padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        )  {
            Column {
                Text(
                    text = "ULS",
                    fontSize = 40.sp,
                    fontFamily = appMono,
                )
                Text(
                    text = "Uniform Leveled Scaling",
                    fontSize = 16.sp,
                    fontFamily = appSans,
                )
            }
            Text(
                text = results.uls.toString(),
                fontSize = 60.sp,
                color = MaterialTheme.colors.primaryVariant,
                fontFamily = appMono,
            )
        }
        Spacer(Modifier.height(40.dp))
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "RFWLS",
                    fontSize = 40.sp,
                    fontFamily = appMono,
                )
                Text(
                    text = "Reciprocal Frequency \nWeighted Leveled Scaling",
                    fontSize = 16.sp,
                    fontFamily = appSans,
                )
            }
            Text(
                text = results.rfwls.toString(),
                fontSize = 60.sp,
                color = MaterialTheme.colors.primaryVariant,
                fontFamily = appMono,
            )
        }
    }
}

val theSuspiciousWord = "mörön"

@Composable
fun RecallGameplayPage() {
    Box(
        Modifier.fillMaxSize()
            .padding(top = 20.dp, bottom = 60.dp, start = 20.dp, end = 20.dp)
    ) {
        val recallGameplayState by recallGameplayState
        val exitButtonShade by animateFloatAsState(
            if (recallGameplayState.result_available) 1f else 0f)
        val exitButtonScale by animateFloatAsState(
            if (recallGameplayState.result_available) 0.75f else 2f)
        val coroutineScope = rememberCoroutineScope()
        AnimatedContent(
            targetState = recallGameplayState,
            transitionSpec = { (slideInHorizontally { x -> x } + fadeIn())
                .togetherWith(slideOutHorizontally { x -> -x } + fadeOut()) },
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
        ) {
            Box {
                Text(
                    text = it.question,
                    fontSize = 80.sp,
                    fontFamily = appSans,
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = "\uf118",
                fontSize = 160.sp,
                fontFamily = appMono,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        updateRecallSession(true)
                    }
                }
            )
            Text(
                text = " \udb82\ude48 ",
                fontSize = 160.sp,
                fontFamily = appMono,
                color = MaterialTheme.colors.primaryVariant,
                modifier = Modifier.alpha(exitButtonShade).scale(exitButtonScale).clickable {
                    coroutineScope.launch {
                        finishSession()
                    }
                }
            )
            Text(
                text = "\uf119",
                fontSize = 160.sp,
                fontFamily = appMono,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        updateRecallSession(false)
                    }
                }
            )
        }
    }
}


suspend fun updateRecallSession(recall: Boolean) {
    val sessionId by sessionIdState
    var state by recallGameplayState
    httpClient.post("http://$targetHost/submit") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf(
            "action" to "choose",
            "recall" to recall.toString()
        )))
    }
    val rState = httpClient.post("http://$targetHost/state") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf()))
    }.body<Message>()
    state = RecallGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        question = rState.details["question"]!!,
    )
}