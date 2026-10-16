package com.phenomena.study.learning

import com.phenomena.study.model.Lesson
import com.phenomena.study.model.Beat

/**
 * University reorganizes 355 existing lessons into guided teaching chapters
 * without presenting editorial rewriting as primary-book quotations.
 *
 * Case studies are newly authored instructional material, while the lesson's
 * exposition/arguments remain identifiable as preserved PHENOMENA commentary.
 */
data class TeachingStage(
    val id:String,
    val title:String,
    val kind:String,
    val instruction:String,
    val transcript:String,
    val checkpoint:String
)
data class ConceptCase(
    val id:String,
    val name:String,
    val triggers:List<String>,
    val scenario:String,
    val distinction:String,
    val objection:String,
    val exercise:String
)

object UniversityPedagogy {
    val caseStudies = listOf(
        ConceptCase("intentionality","Intentionality and its object",
            listOf("intentional","noema","act of consciousness","directedness"),
            "Imagine remembering a friend who is not in the room. You can distinguish the remembering from its remembered object even though the friend is absent. If you now perceive your friend entering, the mode of givenness changes without requiring you to exchange one inner picture for another. The same object may be intended through radically different acts: remembering, expecting, doubting, seeing. This ordinary difference is an entry into intentional analysis.",
            "Separate the occurrence of an act, the manner of intending, and the object as meant. The existence of the intended thing is not a necessary condition of the act's having determinate intentional character. But do not infer that the object must therefore be a small item concealed inside consciousness.",
            "If intentionality only means that every act is about something, does the thesis offer explanatory content? A more adequate analysis tests how empty intending differs from intuitive fulfillment, and whether modes of evidence change with these differences.",
            "Write three sentences about one object: 'I expect it', 'I remember it', 'I perceive it'. Which features vary, and what remains identifiable?"),
        ConceptCase("epoché","The epoché and reduction",
            listOf("epoché","epoche","bracket","reduction","natural attitude"),
            "Consider looking through a window at a tree. In everyday life you simply take it to be there. A phenomenological redirection does not require you to deny the tree's reality. Instead, you investigate how it is given as one enduring tree through changing profiles: from the front, from the side, in memory, and in expectation. The task is to describe the achievement of objectivity without taking an unexplained world-thesis as the beginning of the analysis.",
            "Distinguish suspending a premise for inquiry from affirming its opposite. Bracketing the world's unexamined positing is not identical with skepticism about whether trees exist, and it does not by itself entail a completed doctrine of transcendental idealism.",
            "Can an embodied investigator really suspend world-involvement? That challenge marks the point where Heideggerian objections become methodologically substantive rather than merely dismissive.",
            "Describe an ordinary thing twice: once as a worldly fact, once as a series of appearances and horizons. What did the second description disclose?"),
        ConceptCase("time","Retention and protention",
            listOf("retention","protention","tempor","time-conscious","inner time"),
            "Hear three notes of a familiar melody. The second does not simply replace the first like a new photograph covering an old one. The just-heard note remains relevant as just-past, while the next phrase is anticipated. If every phase were an isolated now, hearing a melody rather than unrelated noises would be difficult to account for. The phenomenological question concerns the structure of temporal appearing itself.",
            "Distinguish primary retention from deliberate recollection. Both relate to the past, but recalling yesterday's event is not the same kind of experience as hearing the immediately preceding note in the ongoing musical phrase.",
            "Does anticipating the next note already involve an explicit prediction? Not necessarily. Protention can concern an indeterminate horizon of what is about to occur, rather than a conscious assertion about the future.",
            "Listen to a simple sequence and describe the different temporal roles of the present tone, the just-elapsed one, and the expected continuation."),
        ConceptCase("being","Being and entities",
            listOf("being","dasein","ontolog","existence","existential"),
            "A carpenter can identify a hammer, describe its material, and measure its weight. Yet using it in a workshop involves another dimension: the hammer is intelligible through tasks, purposes, and a background of equipment. Heidegger's question is not merely whether the object is physically present. It concerns how entities are disclosed as intelligible in the first place and how the investigating human existence is already implicated in this intelligibility.",
            "Distinguish an entity from its Being, and an inventory of properties from an analysis of intelligibility. The ontological difference is not a claim that Being is one especially mysterious entity hidden behind the others.",
            "Can one draw an inference from the human experience of practical involvement to an account of Being as such? This is not answered by slogan. It is a central challenge to the reach of fundamental ontology.",
            "Choose one tool and contrast how it appears during use, after malfunction, and under scientific measurement."),
        ConceptCase("body","Bodily intentionality",
            listOf("body","embod","percept","motor","flesh"),
            "Reaching for a cup usually does not involve first calculating the position of each joint and then translating coordinates into a motor command. Your body already has a meaningful orientation toward possible action. A familiar staircase, instrument, or doorway may appear as something that can be negotiated even before explicit measurement. Altering ability, posture, or habit changes this field of possibilities.",
            "Distinguish the lived body as an orientation of action from the body described as one physical object among others. These are descriptive perspectives, not proof of two independent bodily substances.",
            "A critic might say that neuroscience can explain all the relevant action. The phenomenological reply must specify which features of lived orientation the explanation ought to account for, rather than using the word embodiment as a substitute for evidence.",
            "Reach for an object, interrupt your movement, and describe which expectations and capacities became noticeable only during the interruption."),
        ConceptCase("empathy","Empathy and the other",
            listOf("empath","other person","intersubject","alterity"),
            "When another person is grieving, their expression is not normally encountered as a mere arrangement of colored surfaces. Nor do you have their grief as if it were your own immediate experience. You can recognize an expressive life without replacing it with private projection. This gap between access and non-identity is central to the phenomenology of empathy.",
            "Separate the givenness of another person as experiencing from first-person ownership of that experience. Empathy does not require the disappearance of the distinction between self and other.",
            "A convincing performance could mislead you. Does the possibility of error demonstrate that interpersonal understanding is always a hidden inference? This question should be argued, not assumed.",
            "Describe an encounter in which another's mood became apparent. Distinguish what was expressed, what you inferred, and what remained uncertain."),
        ConceptCase("lifeworld","The lifeworld",
            listOf("lifeworld","life-world","crisis","science"),
            "A map represents a place through coordinates, yet people inhabit it through paths, habits, histories, and expectations. Scientific measurements can be indispensable, but the questions that make measurements relevant arise in a world already meaningful for embodied subjects. The lifeworld can be investigated as a horizon of taken-for-granted validity and shared practices without treating scientific explanation as worthless.",
            "Distinguish the pretheoretical ground of meaning from a refusal of theory. Husserl's lifeworld is not merely an individual's opinions, nor a romantic opposite to rational inquiry.",
            "Do different social groups inhabit entirely separate lifeworlds, or do their historically shaped horizons also support the possibility of mutual understanding? Treat this as a substantive interpretive question.",
            "Select one scientific fact about an everyday practice. Describe how it arises within an already meaningful world of action."),
        ConceptCase("passivity","Passive synthesis",
            listOf("passive","synthesis","association","habit","genesis"),
            "You may hear a recurring rhythm and immediately expect its continuation without actively deciding to predict it. A face can become familiar, a path navigable, and an object recognizable through acquired associations. The investigation of passive synthesis examines forms of unity and anticipation that structure experience before deliberate judgment becomes thematic.",
            "Distinguish passive organization from mere mechanical causation. Calling a synthesis passive does not mean it is a random external effect devoid of sense.",
            "Can an account of passive synthesis explain the historical formation of norms and habits without reducing their validity to psychology? This is an important methodological boundary.",
            "Record a habitual expectation that failed today. Describe what had been anticipated before you consciously formulated an expectation."),
        ConceptCase("self","Pre-reflective self-awareness",
            listOf("self-awareness","self-conscious","pre-reflective","ego","subject"),
            "During absorbed reading you do not ordinarily produce a second thought that observes every first thought. Yet when interrupted, you can report the experience as something you were undergoing. The question is whether this implies an implicit self-presence in the original experience, and if so how it differs from an objectifying act of introspection.",
            "Distinguish thematic reflection on oneself from the possible pre-reflective character of experiencing. One must not confuse the absence of explicit self-description with the absence of experiential first-personality.",
            "Could the apparent first-person unity be reconstructed only through memory and narrative? A phenomenological defense must consider pathological, temporal and embodied counterexamples.",
            "Describe being absorbed in music, then reflecting upon it. What changed when the experience became an object of attention?"),
        ConceptCase("anxiety","Anxiety and the everyday",
            listOf("anxiety","angst","uncann","death","everyday","authentic"),
            "A practical fear has a determinate object: a dangerous animal or a pending exam. Anxiety, in one Heideggerian analysis, is distinguished by a disturbance of the usual meaningfulness of the world rather than by another ordinary object of fear. This distinction is philosophical, not a clinical diagnosis of anxiety disorders.",
            "Separate ordinary factual distress from the existential structure under investigation. Philosophical analysis should not romanticize suffering or imply that existential insight requires mental illness.",
            "Is the distinction between anxiety and fear sustainable across different cultures or individual lives? The examples may challenge overly universal descriptions.",
            "Compare a definite concern with a moment of general estrangement, only if comfortable doing so. Identify what was determinate and what resisted ordinary description."),
        ConceptCase("ethics","The ethical encounter",
            listOf("ethic","face","responsib","levinas","otherness"),
            "Another person may interrupt the tendency to treat everything as something to be surveyed, classified, or possessed. Levinas's account of the face-to-face encounter presses the claim that responsibility can resist reduction to a neutral inventory of properties. The 'face' should not be understood only as a visible collection of facial features.",
            "Distinguish the other's demand from the empirical characteristics by which a person might be identified. Ethical significance is not simply another perceptual property.",
            "How can a claim about unlimited responsibility coexist with finite institutions and obligations to multiple others? The political and ethical implications require further mediation.",
            "Examine a case where treating a person as a category conflicts with the demand of an actual encounter. State the argument without relying on sentiment alone."),
        ConceptCase("method","Description, evidence and method",
            listOf("method","evidence","phenomenolog","intuition","description","rigor"),
            "A researcher can describe an experience with impressive vividness and still misidentify its structure. Phenomenology requires disciplined attention to variations, possible counterexamples, and the difference between what is experienced and what is inferred. Naming a feeling does not establish a universal eidetic law, and philosophical rigor requires more than a confident description.",
            "Distinguish first-person description, structural inference, empirical explanation, and metaphysical interpretation. Arguments may move between these levels, but the transitions need justification.",
            "A skeptic can ask whether introspective findings are sufficiently intersubjective to support robust philosophical claims. A careful reply examines shared criteria, variation, and the role of criticism rather than declaring first-person reports infallible.",
            "Take one familiar phenomenon, vary its circumstances imaginatively, and list the features you think remain necessary. Then try to produce a counterexample.")
    )

    fun caseFor(lesson:Lesson):ConceptCase {
        val text=listOf(lesson.title,lesson.section,lesson.tags.joinToString(" "),
            lesson.synopsis.take(200)).joinToString(" ").lowercase()
        return caseStudies.firstOrNull { c -> c.triggers.any { text.contains(it) } }
            ?:caseStudies.last()
    }

    /** Twelve cumulative, TTS-ready movements per original lesson.
     * The input lesson fields are recontextualized for teaching, not replaced.
     */
    fun stages(lesson:Lesson):List<TeachingStage> {
        val case=caseFor(lesson)
        val steps=mutableListOf<TeachingStage>()
        fun add(id:String,title:String,kind:String,setup:String,content:String,check:String) {
            val spoken=content.trim()
            if(spoken.isNotBlank()) steps +=TeachingStage(id,title,kind,setup,
                "$title. $setup\n\n$spoken\n\nPause for a moment. $check",check)
        }
        add("01","The problem worth asking","ORIENTATION",
            "Begin with an actual intellectual difficulty, not a definition to memorize.",
            "Today's investigation is ${lesson.title}. We are working within ${lesson.section}. Here is the guiding problem in the form presented by the PHENOMENA study commentary.\n\n${lesson.synopsis}",
            "Could you restate the guiding problem as one clear question?")
        add("02","Begin with experience","ILLUSTRATION",
            "Before introducing specialized terms, examine a situation in which the difficulty can be felt.",
            case.scenario,
            "Which aspect of the example is being described rather than explained?")
        add("03","The decisive distinction","FOUNDATION",
            "We now introduce a distinction that prevents a common misunderstanding.",
            case.distinction,
            "What would go wrong if the two sides of this distinction were treated as identical?")
        add("04","The conceptual ladder","SCAFFOLD",
            "Build the argument in small steps. Each concept should make the next one more intelligible.",
            lesson.scaffold.mapIndexed { i,sc ->"Step ${i+1}: ${sc.title}. ${sc.body}" }.joinToString("\n\n")
                .ifBlank{"The fundamental vocabulary here is ${lesson.tags.joinToString(", ")}."},
            "Explain the next step without repeating the wording of the definition.")
        add("05","Work through the exposition","CLOSE READING",
            "We now turn to the sustained argument. Keep the question and distinction in mind.",
            lesson.exposition,
            "Which sentence states the central claim? Which provides the reason for it?")
        add("06","Reconstruct the reasoning","ARGUMENT",
            "Avoid confusing a sequence of paragraphs with a valid inference. Rebuild the logic.",
            lesson.argument.mapIndexed {i,step->"${i+1}. ${step.title}. ${step.detail}"}.joinToString("\n\n")
                .ifBlank { lesson.advanced },
            "Identify one hidden assumption needed for the conclusion to follow.")
        add("07","Put the theory to work","APPLICATION",
            "A concept is not mastered until you can use it to illuminate a different case.",
            lesson.example.ifBlank{case.scenario},
            "Would the concept still apply if the example changed in one important respect?")
        add("08","Advance beyond first definitions","ADVANCED",
            "The first account was necessary, but it may not be sufficient.",
            lesson.advanced,
            "What does the advanced version explain that the elementary version leaves unclear?")
        add("09","Confront a serious objection","CRITICISM",
            "Present the strongest relevant challenge fairly before considering a response.",
            case.objection+"\n\nThe course's critical reading adds:\n"+lesson.critique,
            "Could someone who rejects this view still accept its description of the evidence?")
        add("10","Compare traditions","COMPARISON",
            "Do not collapse a contrast between thinkers into a simple subject–object opposition.",
            lesson.comparison,
            "Identify a real disagreement in starting premises, not merely different vocabulary.")
        add("11","The methodological laboratory","FIELD WORK",
            "Try an exercise before reaching a final conclusion.",
            case.exercise+"\n\nIf the first attempt fails, write down what the failure shows about your initial assumption.",
            "What counterexample would most strongly change your mind?")
        add("12","Teach it back","RECAP",
            "The aim is independent understanding rather than immediate familiarity.",
            "Without consulting the text, explain the question, the leading distinction, the argument, and the principal objection. Then formulate your strongest remaining uncertainty. The essential study-source orientation for this unit is: ${lesson.sourceNote}",
            "Can you explain the problem to another reader without relying on technical jargon?")
        return steps
    }
    fun transcript(lesson:Lesson) = stages(lesson).joinToString("\n\n") { it.transcript }
    fun beats(lesson:Lesson, split:(String)->List<Beat>) =
        stages(lesson).flatMap{stage->
            split(stage.transcript).mapIndexed{i,b->b.copy(
                title=if(i==0)stage.title else "${stage.title} · ${i+1}",
                kind="UNIVERSITY · ${stage.kind}"
            )}
        }
}
