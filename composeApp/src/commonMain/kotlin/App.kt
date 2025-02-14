import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    MassRecallGameplay,
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
    val uls: Int? = null,
    val rfwls: Int? = null,
    val heu: Int? = null,
    val tyv: Int? = null,
)

lateinit var resultsState: MutableState<Results>

data class RecallGameplayState(
    val result_available: Boolean,
    val question: String,
)

lateinit var recallGameplayState: MutableState<RecallGameplayState>

data class MassRecallGameplayState(
    val result_available: Boolean,
    val inverted: Boolean,
    val questions: List<String>,
    var choices: List<Boolean>,
)

lateinit var massRecallGameplayState: MutableState<MassRecallGameplayState>

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
        false, "", listOf(), 0
    )) }
    resultsState = remember { mutableStateOf(Results()) }
    recallGameplayState = remember { mutableStateOf(RecallGameplayState(false, "")) }
    massRecallGameplayState = remember { mutableStateOf(MassRecallGameplayState(
        false, false, listOf(), listOf()
    )) }

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
            Page.MassRecallGameplay -> 30f
            Page.Result -> 30f
        })
        val subtitleSize by animateFloatAsState(
            when(currentPage) {
                Page.Greeting -> 60f
                Page.StandardGameplay -> 30f
                Page.RecallGameplay -> 40f
                Page.MassRecallGameplay -> 40f
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
                    Page.RecallGameplay -> "   Do you recall it?"
                    Page.MassRecallGameplay -> "   Do you recall them?"
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
                Page.RecallGameplay -> RecallGameplayPage()
                Page.MassRecallGameplay -> MassRecallGameplayPage()
                Page.Result -> ResultPage()
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
        Spacer(Modifier.height(15.dp))
        Text(
            text = "Standard word quiz \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchStandardSession()
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Binary recall test \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchRecallSession()
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Test-Your-Vocabulary \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchTyvRecallSession()
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Skimming recall test \uDB80\uDC54",
            fontSize = 40.sp,
            fontFamily = appMono,
            color = MaterialTheme.colors.primary,
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    launchMassRecallSession()
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


suspend fun launchTyvRecallSession() {
    var currentPage by currentPageState
    var sessionId by sessionIdState
    var recallGameplayState by recallGameplayState
    val rStart = httpClient.post("http://$targetHost/start") {
        contentType(ContentType.Application.Json)
        setBody(Message(0, hashMapOf("kind" to "recall-tyv")))
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

suspend fun launchMassRecallSession() {
    var currentPage by currentPageState
    var sessionId by sessionIdState
    var gameplayState by massRecallGameplayState
    val rStart = httpClient.post("http://$targetHost/start") {
        contentType(ContentType.Application.Json)
        setBody(Message(0, hashMapOf("kind" to "recall-mass")))
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
    val questions = rState.details["questions"]!!.split(";;;")
    gameplayState = MassRecallGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        inverted = false,
        questions = questions,
        choices = List(questions.size) { false }
    )
    currentPage = Page.MassRecallGameplay
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
        uls = rFinish.details["uls"]?.toInt(),
        rfwls = rFinish.details["rfwls"]?.toInt(),
        heu = rFinish.details["heu"]?.toInt(),
        tyv = rFinish.details["tyv"]?.toInt(),
    )
    currentPage = Page.Result
}

@Composable
fun ResultPage() {
    val results by resultsState
    Column(
        Modifier.padding(horizontal = 20.dp)
    ) {
        if (results.uls != null) {
            Spacer(Modifier.height(40.dp))
            ResultPageEntry("UniLS", "Uniform Leveled Scaling", results.uls!!)
        }
        if (results.rfwls != null) {
            Spacer(Modifier.height(40.dp))
            ResultPageEntry("ReFLS", "Reciprocal Frequency \nWeighted Leveled Scaling", results.rfwls!!)
        }
        if (results.heu != null) {
            Spacer(Modifier.height(40.dp))
            ResultPageEntry("ReHEU", "Reciprocal Heuristic Estimation", results.heu!!)
        }
        if (results.tyv != null) {
            Spacer(Modifier.height(40.dp))
            ResultPageEntry("ML-TYV",
                "Machine learning based \nmimicry of Test-Your-Vocab scoring", results.tyv!!)
        }
    }
}

@Composable
fun ResultPageEntry(abbr: String, desc: String, value: Int) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = abbr,
                fontSize = 40.sp,
                fontFamily = appMono,
            )
            Text(
                text = desc,
                fontSize = 16.sp,
                fontFamily = appSans,
            )
        }
        Text(
            text = value.toString(),
            fontSize = 60.sp,
            color = MaterialTheme.colors.primaryVariant,
            fontFamily = appMono,
        )
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

lateinit var massRecallGameplayPageCurtain: MutableState<Boolean>

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MassRecallGameplayPage() {
    massRecallGameplayPageCurtain = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var gameplayState by massRecallGameplayState
    var curtain by massRecallGameplayPageCurtain
    val pageAlpha by animateFloatAsState(if (curtain) 0.0f else 1.0f)
    val coroutineScope = rememberCoroutineScope()
    Box(Modifier.padding(bottom = 36.dp)) {
        Box(Modifier.fillMaxSize().alpha(pageAlpha)) {
            AnimatedVisibility(
                visible = scrollState.canScrollBackward,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                    val brush = Brush.verticalGradient(listOf(
                        Color.Black.copy(alpha = 0.125f),
                        Color.White,
                    ))
                    drawRect(
                        topLeft = Offset(-5.0f, 0.0f),
                        size = Size(size.width + 10.0f, size.height),
                        brush = brush
                    )
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = scrollState.canScrollForward,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                    val brush = Brush.verticalGradient(listOf(
                        Color.White,
                        Color.Black.copy(alpha = 0.125f),
                    ))
                    drawRect(
                        topLeft = Offset(-5.0f, 0.0f),
                        size = Size(size.width + 10.0f, size.height),
                        brush = brush
                    )
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            val invertButtonColor by animateColorAsState(
                if (gameplayState.inverted) MaterialTheme.colors.primary else Color.Black)
            Box(Modifier.clip(RoundedCornerShape(25)).clickable {
                gameplayState = gameplayState.copy(inverted = !gameplayState.inverted)
            }) {
                val checkbox = if (gameplayState.inverted) "\udb80\udd35" else "\udb80\udd31"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = checkbox,
                        fontFamily = appMono,
                        fontSize = 40.sp,
                        color = invertButtonColor
                    )
                    Spacer(Modifier.width(20.dp))
                    Text(
                        text = "I choose what I don't know!",
                        fontFamily = appMono,
                        fontSize = 24.sp,
                        color = invertButtonColor
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow {
                gameplayState.questions.indices.forEach { i ->
                    val weight by animateIntAsState(
                        if (gameplayState.choices[i]) FontWeight.Bold.weight else FontWeight.Normal.weight
                    )
                    Text(
                        text = gameplayState.questions[i],
                        fontFamily = appSans,
                        fontSize = 20.sp,
                        fontWeight = FontWeight(weight),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            .clickable {
                                gameplayState = gameplayState.copy(
                                    choices = gameplayState.choices.toMutableList().apply {
                                        this[i] = !this[i]
                                    }
                                )
                            }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { coroutineScope.launch {
                        curtain = true
                        delay(100)
                        updateMassRecallSession()
                        curtain = false
                        delay(100)
                    } }
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 30.sp,
                        fontFamily = appMono
                    )
                }
                if (gameplayState.result_available) TextButton(
                    onClick = { coroutineScope.launch {
                        finishSession()
                    } }
                ) {
                    Text(
                        text = "Finish",
                        fontSize = 30.sp,
                        fontFamily = appMono
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

suspend fun updateMassRecallSession() {
    val sessionId by sessionIdState
    var state by massRecallGameplayState
    val choices = state.choices
        .map { if (state.inverted) !it else it }
        .joinToString(separator = ",")
    httpClient.post("http://$targetHost/submit") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf(
            "action" to "choose",
            "choices" to choices,
        )))
    }
    val rState = httpClient.post("http://$targetHost/state") {
        contentType(ContentType.Application.Json)
        setBody(Message(sessionId.toLong(), hashMapOf()))
    }.body<Message>()
    val questions = rState.details["questions"]!!.split(";;;")
    state = MassRecallGameplayState(
        result_available = rState.details["result_available"]!!.toBoolean(),
        inverted = state.inverted,
        questions = questions,
        choices = List(questions.size) { false }
    )
}