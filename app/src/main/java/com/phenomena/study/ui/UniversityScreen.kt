package com.phenomena.study.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phenomena.study.data.*
import com.phenomena.study.learning.*
import com.phenomena.study.model.Lesson
import com.phenomena.study.player.StudyPlayer
import com.phenomena.study.player.toSpeechChunks
import com.phenomena.study.service.SeminarPlaybackService

/** A curriculum, not a second text dump: concepts are introduced from lived
 * examples and progressively reconstructed through twelve teaching movements.
 * All 355 lesson courses are generated from their actual work and source notes.
 */
@Composable
fun UniversityScreen(
    catalog:Catalog,
    currentLesson:Lesson,
    player:StudyPlayer,
    onSetCurrentStudy:(String)->Unit,
    onOpenPlayer:()->Unit,
    onSaveNote:(String,String)->Unit
) {
    val context=LocalContext.current
    var workId by rememberSaveable { mutableStateOf(currentLesson.workId) }
    var selectedId by rememberSaveable { mutableStateOf(currentLesson.id) }
    var coursePicker by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    var unfoldedStage by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    val lesson=catalog.lesson(selectedId)?.takeIf{it.workId==workId}
        ?:catalog.lessons.firstOrNull{it.workId==workId}
        ?: currentLesson
    val work=catalog.work(lesson.workId)
    val related=remember(workId,catalog.lessons) {catalog.lessons.filter{it.workId==workId}}
    val stages=remember(lesson.id){UniversityPedagogy.stages(lesson)}
    val words=remember(lesson.id){UniversityPedagogy.transcript(lesson).trim().split(Regex("\\s+")).size}
    val cases=remember(lesson.id){UniversityPedagogy.caseFor(lesson)}
    val filteredWorks=remember(filter){catalog.works.filter {
        it.title.contains(filter,true)||it.author.contains(filter,true)
    }}
    var unitPick by remember {mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=19.dp,vertical=19.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item {
            Eyebrow("PHENOMENA UNIVERSITY · LECTURES AND TUTORIALS")
            Spacer(Modifier.height(6.dp))
            Display("The university of\nexperience.",size=35)
            Text("A structured academy for learning concepts before confronting technical arguments.",
                color=Muted,fontSize=12.sp,lineHeight=20.sp)
        }
        item {
            Box(Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF233B3D),Color(0xFF14232F))),
                RoundedCornerShape(25.dp))) {
                Column(Modifier.padding(19.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text("YOUR ACADEMIC COLLECTION",fontSize=10.sp,color=Brass,letterSpacing=1.3.sp)
                    Text("${catalog.works.size} courses · ${catalog.lessons.size} teaching units",
                        fontFamily=FontFamily.Serif,fontSize=26.sp,color=Color.White,lineHeight=31.sp)
                    Text("Twelve teaching movements per unit · original conceptual demonstrations · "
                        +"guided exposition · critical exercises",
                        fontSize=11.sp,color=Color.White.copy(alpha=.75f),lineHeight=18.sp)
                    LinearProgressIndicator(progress={
                        related.indexOfFirst {it.id==lesson.id}.coerceAtLeast(0).toFloat()/
                            (related.size-1).coerceAtLeast(1)
                    },color=Mint,trackColor=Color.White.copy(alpha=.2f),
                        modifier=Modifier.fillMaxWidth())
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("I · CHOOSE YOUR SCHOOL")
                OutlinedButton(onClick={coursePicker=!coursePicker},
                    modifier=Modifier.fillMaxWidth()){
                    Text("${work?.title ?: "Select a work"} · ${work?.author.orEmpty()}",
                        modifier=Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
                    Icon(Icons.Default.ExpandMore,null)
                }
                if(coursePicker) {
                    OutlinedTextField(value=filter,onValueChange={filter=it},
                        singleLine=true,modifier=Modifier.fillMaxWidth(),
                        placeholder={Text("Search works or philosophers")})
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=270.dp)){
                        items(filteredWorks,key={it.id}) { option->
                            TextButton(onClick={
                                workId=option.id
                                selectedId=catalog.lessons.firstOrNull { it.workId==option.id }?.id
                                    ?:currentLesson.id
                                unfoldedStage=null;coursePicker=false;unitPick=false
                            },modifier=Modifier.fillMaxWidth()){
                                Column(Modifier.fillMaxWidth()){
                                    Text(option.title,fontSize=14.sp,color=Ink)
                                    Text(option.author,fontSize=11.sp,color=Muted)
                                }
                            }
                        }
                    }
                }
                Eyebrow("II · CHOOSE YOUR LECTURE")
                OutlinedButton(onClick={unitPick=!unitPick},modifier=Modifier.fillMaxWidth()){
                    Text("${lesson.section} · ${lesson.title}",maxLines=2,
                        modifier=Modifier.weight(1f),overflow=TextOverflow.Ellipsis)
                    Icon(Icons.Default.ExpandMore,null)
                }
                if(unitPick) {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max=290.dp)){
                        itemsIndexed(related,key={_,l->l.id}) { index,item->
                            TextButton(onClick={
                                selectedId=item.id
                                unitPick=false
                                unfoldedStage=null
                            },modifier=Modifier.fillMaxWidth()){
                                Column(Modifier.fillMaxWidth()){
                                    Text("Lecture ${index+1}. ${item.title}",
                                        color=Ink,fontSize=13.sp,maxLines=2)
                                    Text(item.section,fontSize=10.sp,color=Muted)
                                }
                            }
                        }
                    }
                }
                Button(onClick={
                    onSetCurrentStudy(lesson.id)
                },modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.BookmarkAdded,null,Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Make this my Current Study")
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("TODAY'S TEACHING UNIT")
                Text(lesson.title,color=Ink,fontSize=24.sp,lineHeight=30.sp,
                    fontFamily=FontFamily.Serif)
                Text("${stages.size} movements · ${words} transcript words · "
                    +"${"%.1f".format(words/125.0)} min estimated at 125 WPM",
                    color=Brass,fontSize=11.sp,lineHeight=17.sp)
                Text("Edition guidance: ${lesson.sourceNote}",color=Muted,
                    fontSize=10.sp,lineHeight=16.sp)
                Text("Pedagogically reorganized PHENOMENA commentary. Not a recording "
                    +"or reproduction of a licensed primary-book edition.",
                    color=Muted,fontSize=11.sp,lineHeight=17.sp)
                Button(onClick={
                    SeminarPlaybackService.send(
                        context,
                        SeminarPlaybackService.ACTION_STOP)
                    val beats=UniversityPedagogy.beats(lesson) {t->toSpeechChunks(t,2000)}
                    player.stop();player.setSegments(beats)
                    player.autoAdvance=true;player.play()
                },modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Default.PlayArrow,null)
                    Spacer(Modifier.width(7.dp));Text("Listen to full teaching lecture")
                }
                Text(player.status,color=Muted,fontSize=11.sp)
                if(player.speaking) Row(Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically){
                    Text("${player.beatIndex+1} / current narration chunks",
                        color=Mint,fontSize=11.sp)
                    TextButton(onClick={player.stop()}){Text("Stop lecture")}
                }
                OutlinedButton(onClick=onOpenPlayer,modifier=Modifier.fillMaxWidth()){
                    Text("Open unified listening room",modifier=Modifier.weight(1f))
                    Icon(Icons.Default.Headphones,null)
                }
            }
        }
        item {
            RowHeadline("The teaching sequence","${stages.size} MOVEMENTS")
            Text("Open each stage to read its complete pedagogical transcript "
                +"and self-test. Lectures build cumulatively.",fontSize=11.sp,color=Muted)
        }
        items(stages,key={it.id}) { stage ->
            Surface(onClick={
                unfoldedStage=if(unfoldedStage==stage.id)null else stage.id
            },color=CardSurface,shape=RoundedCornerShape(18.dp),
                border=BorderStroke(1.dp,Line),modifier=Modifier.fillMaxWidth()){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(stage.id,color=Brass,fontSize=14.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){
                            Text(stage.title,color=Ink,fontSize=16.sp,
                                fontFamily=FontFamily.Serif,lineHeight=22.sp)
                            Text(stage.kind,color=Muted,fontSize=10.sp,letterSpacing=.8.sp)
                        }
                        Icon(if(unfoldedStage==stage.id)Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,null,tint=Mint)
                    }
                    if(unfoldedStage==stage.id){
                        Rule()
                        Text(stage.instruction,color=Mint,fontSize=12.sp,lineHeight=18.sp)
                        SelectionContainer {
                            Text(stage.transcript,color=Ink,fontSize=14.sp,
                                lineHeight=23.sp,fontFamily=FontFamily.Serif)
                        }
                        Box(Modifier.fillMaxWidth().background(Cream,RoundedCornerShape(12.dp))
                            .padding(13.dp)) {
                            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Eyebrow("SPEAK IT BACK")
                                Text(stage.checkpoint,color=Ink,fontSize=12.sp,lineHeight=19.sp)
                            }
                        }
                        OutlinedButton(onClick={
                            val beats=toSpeechChunks(stage.transcript,maxChunk=2000)
                            SeminarPlaybackService.send(
                                context,
                                SeminarPlaybackService.ACTION_STOP)
                            player.stop();player.setSegments(beats);player.autoAdvance=true;player.play()
                        },modifier=Modifier.fillMaxWidth()){
                            Icon(Icons.Default.VolumeUp,null,Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp));Text("Listen to this movement")
                        }
                    } else {
                        Text(stage.instruction,color=Muted,fontSize=11.sp,
                            maxLines=2,lineHeight=17.sp)
                    }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()){
                Eyebrow("PRACTICAL SEMINAR · ORIGINAL CASE STUDY")
                Text(cases.name,color=Ink,fontFamily=FontFamily.Serif,fontSize=21.sp)
                Body(cases.scenario)
                Eyebrow("A CONCEPTUAL DISTINCTION")
                Body(cases.distinction)
                Eyebrow("CRITICAL STRESS TEST")
                Body(cases.objection)
                Eyebrow("PERFORM THE EXERCISE")
                Body(cases.exercise)
                Rule()
                OutlinedTextField(value=noteText,onValueChange={noteText=it},
                    modifier=Modifier.fillMaxWidth(),minLines=3,maxLines=8,
                    label={Text("Your interpretation, objection or example")})
                Button(onClick={
                    onSaveNote("University · ${lesson.title}",
                        "Lesson ${lesson.id}; course ${lesson.workId}; case ${cases.id}\n${noteText.trim()}")
                    noteText=""
                },enabled=noteText.isNotBlank(),modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Save,null,Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp));Text("Save university notebook entry")
                }
            }
        }
        item {
            Text("PHENOMENA UNIVERSITY · Pedagogical transformations of the existing "
                +"course material, plus twelve original conceptual case studies. "
                +"No page-level source citation is represented as edition-verified.",
                color=Muted,fontSize=10.sp,lineHeight=16.sp)
        }
    }
}
