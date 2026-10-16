package com.phenomena.study.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.phenomena.study.service.SeminarPlaybackService
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.data.*
import com.phenomena.study.model.Lesson
import com.phenomena.study.player.StudyPlayer
import com.phenomena.study.player.toSpeechChunks
import com.phenomena.study.reader.ParagraphIndex

/**
 * Institute workspace: commentary, independent authored seminars, philology
 * and practical phenomenological/argument reconstruction laboratory.
 * The source edition, import and study guide remain clearly distinct.
 */
@Composable
fun InstituteScreen(
    lesson: Lesson,
    catalog: Catalog,
    imports: List<ImportedDocument>,
    player: StudyPlayer,
    onSaveNote: (String, String) -> Unit,
    onOpenListener: () -> Unit
) {
    var tab by rememberSaveable { mutableStateOf("Commentary") }
    val tabs = listOf("Commentary", "Seminars", "Philology", "Laboratory")
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=20.dp,vertical=22.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)
    ) {
        item {
            Eyebrow("PHENOMENA 3.0 · THE INSTITUTE")
            Spacer(Modifier.height(5.dp))
            Display("The art of interpretation.",size=31)
            Spacer(Modifier.height(6.dp))
            Text("Read closely. Reconstruct faithfully. Object rigorously.",fontSize=12.sp,color=Muted)
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Eyebrow("SELECTED RESEARCH CONTEXT")
                Text(lesson.title,fontSize=21.sp,fontFamily=FontFamily.Serif,color=Ink,lineHeight=27.sp)
                Text("${catalog.work(lesson.workId)?.author} · ${catalog.work(lesson.workId)?.title}",
                    color=Muted,fontSize=12.sp,lineHeight=18.sp)
                Text("All Institute tools are scoped to this study unless you explicitly select an external edition or term.",
                    color=Mint,fontSize=11.sp,lineHeight=17.sp)
            }
        }
        item {
            LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                items(tabs) { option -> LabelTag(option,tab==option) { tab=option } }
            }
        }
        when(tab) {
            "Commentary" -> item { GreatCommentaryPanel(lesson,catalog,onSaveNote) }
            "Seminars" -> item { InstituteSeminarPanel(lesson,catalog,player,onOpenListener) }
            "Philology" -> item { PhilologicalObservatory(imports) }
            "Laboratory" -> item { PhenomenologicalLaboratory(lesson,onSaveNote) }
        }
        item {
            Text("PHENOMENA INSTITUTE · Original educational interpretation. " +
                "Section references are editorial orientation, not verified page quotations.",
                color=Muted,fontSize=10.sp,lineHeight=16.sp)
        }
    }
}

@Composable
private fun GreatCommentaryPanel(
    lesson: Lesson,
    catalog: Catalog,
    onSaveNote: (String,String) -> Unit
) {
    val dossier=InstituteMaterials.dossierFor(lesson.id)
    var expanded by remember(lesson.id) { mutableStateOf("RECONSTRUCTION") }
    var response by remember(lesson.id) { mutableStateOf("") }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE GREAT COMMENTARY 3.0")
        Display(lesson.section,size=24)
        Text(if(dossier!=null) "Authored critical dossier · section-oriented" else
            "Structured reconstruction from the existing lesson · no edition-verified close reading",
            color=Muted,fontSize=11.sp,lineHeight=17.sp)
        Rule()
        Eyebrow("THE CENTRAL PROBLEM")
        Body(dossier?.problem ?: lesson.synopsis)
        Eyebrow("WORKING INTERPRETATION")
        Body(dossier?.thesis ?: lesson.exposition)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("RECONSTRUCTION","RIVAL READING","OBJECTION","RESPONSE","EVIDENCE").forEach {
                LabelTag(it,expanded==it) { expanded=it }
            }
        }
        when(expanded) {
            "RECONSTRUCTION" -> {
                if(dossier!=null) dossier.reconstruction.forEachIndexed { i,p ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("${i+1}.",color=Brass,fontSize=13.sp)
                        Text(p,Modifier.weight(1f),color=Ink,fontSize=13.sp,lineHeight=20.sp)
                    }
                } else lesson.argument.forEachIndexed { i,step ->
                    Eyebrow("${i+1}. ${step.title}")
                    Body(step.detail)
                }
            }
            "RIVAL READING" -> Body(dossier?.interpretiveRival ?: lesson.comparison)
            "OBJECTION" -> Body(dossier?.objection ?: lesson.critique)
            "RESPONSE" -> Body(dossier?.response ?: lesson.advanced)
            "EVIDENCE" -> {
                Body(dossier?.sourceLocator ?: lesson.sourceNote,color=Muted)
                Text("This entry is original commentary, not a verbatim passage from a licensed primary-text edition.",
                    color=Brass,fontSize=11.sp,lineHeight=17.sp)
            }
        }
        Rule()
        OutlinedTextField(value=response,onValueChange={response=it},
            label={Text("Your critical objection or alternative interpretation")},
            minLines=3,maxLines=6,modifier=Modifier.fillMaxWidth())
        Button(onClick={
            onSaveNote("Institute · ${lesson.section} · ${lesson.title}",
                "Work ${lesson.workId}; lesson ${lesson.id}; mode ${expanded}\n${response.trim()}")
            response=""
        },enabled=response.isNotBlank(),modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save,null,Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp)); Text("Save to Research Notebook")
        }
    }
}

@Composable
private fun InstituteSeminarPanel(
    lesson:Lesson,catalog:Catalog,player:StudyPlayer,onOpenListener:()->Unit
) {
    val instituteContext=LocalContext.current
    val lectures=remember(lesson.workId) {InstituteMaterials.lecturesFor(lesson.workId)}
    val modules=remember(lesson.workId,catalog.seminars) {catalog.seminars.filter{it.workId==lesson.workId}}
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE GREAT SEMINAR 3.0")
        Display("Arguments aloud.",size=27)
        Body("Original lectures and structured philosophical debates appear here. " +
            "The preserved long-form library is available in the topic-scoped Listening Room.",
            color=Muted)
        if(lectures.isEmpty()) {
            Text("No new authored lecture for this work yet. Its earlier seminars remain in the listening queue.",
                color=Muted,fontSize=12.sp)
        }
        lectures.forEach { lecture ->
            Surface(color=CardRaised,shape=RoundedCornerShape(17.dp),
                border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Eyebrow(lecture.type+" · NEW AUTHORIAL MATERIAL")
                    Text(lecture.title,color=Ink,fontFamily=FontFamily.Serif,fontSize=20.sp,lineHeight=25.sp)
                    Text(lecture.subtitle,color=Muted,fontSize=12.sp)
                    Text("${lecture.words} spoken words · ~${"%.1f".format(lecture.words/125.0)} min at 125 WPM",
                        color=Mint,fontSize=11.sp)
                    Text(lecture.reference,color=Muted,fontSize=10.sp,lineHeight=16.sp)
                    Button(onClick={
                        player.stop()
                        SeminarPlaybackService.send(instituteContext,
                            SeminarPlaybackService.ACTION_PLAY,listOf(lecture.id))
                    },modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow,null)
                        Text("Listen to authored ${lecture.type.lowercase()}")
                    }
                }
            }
        }
        Rule()
        Text("${modules.size} existing seminar modules for this work",fontSize=13.sp,color=Ink)
        modules.take(6).forEach { seminar ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones,null,tint=Brass,modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text(seminar.title,color=Ink,fontSize=12.sp,modifier=Modifier.weight(1f))
            }
        }
        OutlinedButton(onClick=onOpenListener,modifier=Modifier.fillMaxWidth()) {
            Text("Open background seminar queue")
            Spacer(Modifier.width(7.dp)); Icon(Icons.Default.ArrowForward,null)
        }
        Text("The new authored lectures play through the on-screen Android narrator. " +
            "The existing topic-scoped long-form queue uses the foreground media service.",
            fontSize=11.sp,color=Muted,lineHeight=17.sp)
    }
}

@Composable
private fun PhilologicalObservatory(imports:List<ImportedDocument>) {
    var lexical by rememberSaveable { mutableStateOf("Sein") }
    val term=InstituteMaterials.lexemes.firstOrNull{it.lemma==lexical}
        ?: InstituteMaterials.lexemes.first()
    var leftId by rememberSaveable { mutableLongStateOf(-1L) }
    var rightId by rememberSaveable { mutableLongStateOf(-1L) }
    var leftParagraph by rememberSaveable(leftId) { mutableIntStateOf(0) }
    var rightParagraph by rememberSaveable(rightId) { mutableIntStateOf(0) }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE PHILOLOGICAL OBSERVATORY")
        Display("A word is not a doctrine.",size=25)
        Text("Translation choices are editorial possibilities, not citations from a commercial translation.",
            color=Muted,fontSize=12.sp,lineHeight=18.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            InstituteMaterials.lexemes.forEach { item ->
                LabelTag(item.lemma,item.lemma==lexical) {lexical=item.lemma}
            }
        }
        Rule()
        Text(term.lemma,color=Ink,fontFamily=FontFamily.Serif,fontSize=26.sp)
        Eyebrow(term.language+" · MORPHOLOGY")
        Body(term.morphology)
        Eyebrow("POSSIBLE ENGLISH RENDERINGS")
        term.possibilities.forEach { Text("• $it",color=Mint,fontSize=13.sp) }
        Eyebrow("THE INTERPRETIVE STAKES")
        Body(term.stakes)
        Text(term.caution,color=Muted,fontSize=11.sp,lineHeight=17.sp)
    }
    Spacer(Modifier.height(12.dp))
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("USER-LICENSED PARALLEL READER")
        Text("Select two of your imported editions. Text is displayed side by side in separate panels on wide screens, or sequentially on phones.",
            color=Muted,fontSize=12.sp,lineHeight=18.sp)
        Text("Manual navigation only: paragraph alignment, translation identity and printed edition locations are NOT inferred or verified.",
            color=Brass,fontSize=11.sp,lineHeight=17.sp)
        if(imports.isEmpty()) EmptyNotice("Import your legally acquired EPUB or selectable PDF from the Notebook to compare passages.")
        else {
            ImportComparePane("EDITION A",imports,leftId,{leftId=it},
                leftParagraph,{leftParagraph=it})
            Rule()
            ImportComparePane("EDITION B",imports,rightId,{rightId=it},
                rightParagraph,{rightParagraph=it})
        }
    }
}

@Composable
private fun ImportComparePane(
    name:String,
    imports:List<ImportedDocument>,
    selectedId:Long,onSelect:(Long)->Unit,
    position:Int,onPosition:(Int)->Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected=imports.firstOrNull { it.id==selectedId }
    Eyebrow(name)
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
            Text(selected?.title ?: "Select an imported document",modifier=Modifier.weight(1f),
                maxLines=1,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown,null)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            imports.forEach { imp ->
                DropdownMenuItem(text={Text(imp.title,maxLines=2)},
                    onClick={expanded=false;onSelect(imp.id)})
            }
        }
    }
    if(selected!=null) {
        val doc=remember(selected.id,selected.content) {ParagraphIndex.forImport(selected)}
        val i=position.coerceIn(0,(doc.paragraphs.size-1).coerceAtLeast(0))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Paragraph ${i+1}/${doc.paragraphs.size}",fontSize=11.sp,color=Muted,modifier=Modifier.weight(1f))
            IconButton(onClick={onPosition(i-1)},enabled=i>0) {Icon(Icons.Default.ChevronLeft,"Previous")}
            IconButton(onClick={onPosition(i+1)},enabled=i+1<doc.paragraphs.size) {Icon(Icons.Default.ChevronRight,"Next")}
        }
        SelectionContainer {
            Text(doc.paragraphs.getOrNull(i)?.text ?: "No extractable text",
                color=Ink,fontFamily=FontFamily.Serif,fontSize=15.sp,lineHeight=23.sp)
        }
        Text("Imported source · extracted character offsets are not page citations.",
            color=Muted,fontSize=10.sp)
    }
}

@Composable
private fun PhenomenologicalLaboratory(lesson:Lesson,onSaveNote:(String,String)->Unit) {
    val protocols=InstituteMaterials.protocols
    var chosen by rememberSaveable(lesson.workId) {
        mutableStateOf(protocols.firstOrNull { it.associatedWorkId==lesson.workId }?.id
            ?: protocols.first().id)
    }
    val protocol=protocols.firstOrNull{it.id==chosen}?:protocols.first()
    var description by rememberSaveable(chosen) { mutableStateOf("") }
    var analysis by rememberSaveable(chosen) { mutableStateOf("") }
    var objection by rememberSaveable(chosen) { mutableStateOf("") }
    val context=LocalContext.current
    var dictationTarget by remember { mutableStateOf("description") }
    var voiceMessage by remember { mutableStateOf("") }
    val speechInput=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode==Activity.RESULT_OK) {
            val words=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if(words.isNotBlank()) when(dictationTarget) {
                "analysis" -> analysis=(analysis+" "+words).trim()
                "objection" -> objection=(objection+" "+words).trim()
                else -> description=(description+" "+words).trim()
            }
        }
    }
    fun dictate(target:String) {
        dictationTarget=target
        val intent=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT,"Dictate your philosophical observation")
        }
        try { speechInput.launch(intent) }
        catch (_:Exception) { voiceMessage="No compatible speech recognition service is available." }
    }
    Panel(Modifier.fillMaxWidth()) {
        Eyebrow("THE PHENOMENOLOGICAL LABORATORY")
        Display("Describe before you explain.",size=25)
        Body("Experiments are methodological prompts. Record the experience as presented before importing a preferred philosophical theory.",color=Muted)
        OutlinedButton(onClick={
            SeminarPlaybackService.send(context,SeminarPlaybackService.ACTION_PLAY,
                listOf("lab:${protocol.id}"))
        },modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)){
            Icon(Icons.Default.Headphones,null,Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Listen to guided research exercise")
        }
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            protocols.forEach { LabelTag(it.title,it.id==chosen){chosen=it.id} }
        }
        Rule()
        Eyebrow("I · OBSERVATION")
        Body(protocol.observation)
        OutlinedTextField(value=description,onValueChange={description=it},
            modifier=Modifier.fillMaxWidth(),label={Text("Describe what is given")},minLines=3,maxLines=7)
        OutlinedButton(onClick={dictate("description")},
            modifier=Modifier.fillMaxWidth().heightIn(min=49.dp)){
            Icon(Icons.Default.Mic,null,Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Dictate description (optional)")
        }
        Eyebrow("II · CONCEPTUAL DISTINCTION")
        Body(protocol.distinction)
        OutlinedTextField(value=analysis,onValueChange={analysis=it},
            modifier=Modifier.fillMaxWidth(),label={Text("Reconstruct a provisional argument")},minLines=3,maxLines=7)
        OutlinedButton(onClick={dictate("analysis")},
            modifier=Modifier.fillMaxWidth().heightIn(min=49.dp)){
            Icon(Icons.Default.Mic,null,Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Dictate analysis (optional)")
        }
        Eyebrow("III · COUNTEREXAMPLE")
        Body(protocol.counterexample)
        OutlinedTextField(value=objection,onValueChange={objection=it},
            modifier=Modifier.fillMaxWidth(),label={Text("Test the claim against an objection")},minLines=3,maxLines=7)
        OutlinedButton(onClick={dictate("objection")},
            modifier=Modifier.fillMaxWidth().heightIn(min=49.dp)){
            Icon(Icons.Default.Mic,null,Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("Dictate objection (optional)")
        }
        Button(onClick={
            onSaveNote("Institute Laboratory · ${protocol.title}",
                "Lesson ${lesson.id}; protocol ${protocol.id}\n\nOBSERVATION\n${description.trim()}\n\nINTERPRETATION\n${analysis.trim()}\n\nOBJECTION\n${objection.trim()}")
            description="";analysis="";objection=""
        },enabled=description.isNotBlank() && analysis.isNotBlank(),
            modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save,null,Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp));Text("Save research dossier")
        }
        if(voiceMessage.isNotBlank()) Text(voiceMessage,color=Muted,fontSize=11.sp)
        Text("Speech recognition uses your phone's provider and may require internet. It starts only when you tap Dictate.",
            color=Muted,fontSize=10.sp,lineHeight=16.sp)
        Text("These exercises are not clinical assessments or proof of original research; your notes remain on your device unless you export them.",
            color=Muted,fontSize=10.sp,lineHeight=16.sp)
    }
}
