package com.phenomena.study.data

/** PHENOMENA-authored instruction; not translated or extracted primary-book text.
 * References give work/section orientation, never fabricated page or edition citations. */
data class InstituteLecture(val id:String,val workId:String,val title:String,val subtitle:String,
                            val type:String,val reference:String,val paragraphs:List<String>) {
    val words:Int get() = paragraphs.joinToString(" ").trim().split(Regex("\\s+")).size
}
data class CommentaryDossier(val lessonId:String,val problem:String,val thesis:String,
    val reconstruction:List<String>,val interpretiveRival:String,val objection:String,
    val response:String,val sourceLocator:String)
data class LexicalProblem(val lemma:String,val language:String,val morphology:String,
    val possibilities:List<String>,val stakes:String,val caution:String)
data class LaboratoryProtocol(val id:String,val title:String,val observation:String,
    val distinction:String,val counterexample:String,val associatedWorkId:String)

object InstituteMaterials {
    val lectures = listOf(
        InstituteLecture("v3-lecture-intentionality","logical",
            "Intentionality without the container metaphor",
            "Logical Investigations · Lecture and interpretive controversy","LECTURE",
            "Husserl, Logical Investigations, Fifth Investigation (edition-independent orientation)",listOf(
            "When consciousness is said to be intentional, the claim is not that a mental object sits inside a private container. The claim concerns the directedness of experiencing. Perceiving is perceiving something, judging is judging something to be the case, and hoping is hoping for an outcome. We must avoid confusing the object intended with an additional miniature object hidden in the mind. A judgment about an absent friend and a perception of a present table differ as acts, but both exhibit directedness.",
            "The decisive philosophical question is whether intentionality identifies a psychological relation between two actually existing entities. If I imagine a golden mountain, no golden mountain need exist. Yet the imagining is determinately about something. Therefore a simple relation model, on which both relata must actually exist, cannot explain the structure. The intended object is not thereby reduced to an inner picture. It is the object as intended through a specific mode of presentation.",
            "This problem must also be kept separate from the problem of fulfillment. An empty intention can mean or anticipate something without intuitive presentation, whereas an intuition may give what had merely been meant. The difference between intending and fulfilling supplies one route toward understanding evidence: it is not simply the psychological intensity of certainty, but a distinctive coincidence or synthesis of intended meaning and intuitive givenness.",
            "A central objection says that describing intending as directedness risks explaining nothing: every example merely repeats that thinking is thinking of something. An adequate response must characterize systematic contrasts—empty versus fulfilled, perceptual versus signitive, possible versus impossible intended objects—and show how those contrasts change what counts as an adequate description of an act. The result is methodological rather than a metaphysics of inaccessible inner entities."
        )),
        InstituteLecture("v3-dialogue-reduction","husserl",
            "A dispute about the epoché",
            "Two competing interpretations of the reduction","DIALOGUE",
            "Husserl, Ideas I, passages on the natural attitude, epoché and reduction",listOf(
            "First voice: the natural attitude is the ordinary taking-for-granted of a world that exists independently of any one act of experiencing. The epoché does not simply declare that world nonexistent. It suspends the unexamined use of its existence as a premise within a particular investigation. This is a change in reflective attitude, not a magic departure from embodiment or history.",
            "Second voice: if the world is bracketed, have we not returned to an isolated subject surveying merely mental contents? Why should the method escape subjectivism? A stronger objection asks whether the very possibility of bracketing already presupposes a subject standing apart from relations that constitute it.",
            "First voice: the methodological purpose of bracketing is to make correlation thematic. The perceived tree and the experiencing of the tree are not two items to be separated by a wall. Rather, the analysis asks how the tree is given as stable across varying profiles, horizons and fulfillments. The object is examined precisely as meant and appearing, not merely dismissed as a hallucination.",
            "Second voice: yet a Heideggerian criticism can still contend that the world is already meaningful through situated existence, practical involvement, and historicity. Recasting those relations primarily as intentional constitution risks privileging a theoretical stance. The dispute must not be flattened into the assertion that one thinker cares about subjects and the other about objects. They contest the starting conditions of the philosophical inquiry.",
            "Seminar synthesis: the exact disagreement concerns what is first in the inquiry, what is rendered visible by the method, and whether phenomenological clarification requires a transcendental reorientation. Participants should state which premise they reject before declaring a victory for either tradition."
        )),
        InstituteLecture("v3-lecture-being","heidegger",
            "From beings to the question of Being",
            "Being and Time · Structure, method and the ontological difference","LECTURE",
            "Heidegger, Being and Time, Introduction §§1–7",listOf(
            "The question of Being is easily misunderstood as a request for a list of the properties shared by every entity. Such an approach already assumes that Being is another feature that entities possess. Heidegger instead presses the distinction between what is and the intelligibility of its Being. We encounter tools, people, numbers, events and institutions; describing these items does not by itself settle what it means for them to be.",
            "The question is reflexive because the investigator is not external to it. Dasein names the being for whom its own Being is in question. This formula must not be read as a romantic claim that humans are inherently mysterious. It identifies the existential structure within which inquiry is possible: the questioner already understands Being in a practical, preliminary and usually unthematized way.",
            "The analytic begins from everyday involvement. A hammer encountered in work is not first a neutral object to which a subjective meaning is later attached. Its significance is disclosed within a referential whole of tasks, equipment, purposes and others. This interpretation does not mean that material objects disappear into practical interpretation; it says that a certain kind of intelligibility is methodologically more basic than the representation of bare physical properties.",
            "The strongest caution is against turning this claim into a simple slogan: practice precedes theory. The analytic demands a structural account of how world, understanding, affectivity, discourse and temporality belong together. A reading that merely opposes practical life to theoretical contemplation misses the ambition of fundamental ontology. The question remains whether Heidegger succeeds in deriving the broader ontological project from his description of Dasein."
        )),
        InstituteLecture("v3-dialogue-body","merleau",
            "The body as a condition of appearing",
            "Phenomenology of Perception · Lived embodiment and critique","DIALOGUE",
            "Merleau-Ponty, Phenomenology of Perception, Introduction and Part One",listOf(
            "Opening thesis: in ordinary action the body does not always appear as one object among others. Reaching for a cup involves an operative sense of distance, possibility and coordination that cannot be reconstructed adequately as a sequence of explicit judgments about anatomical segments.",
            "Objection: does this merely introduce an obscure faculty called body schema? If the claim is empirical, experimental psychology can investigate it. If it is transcendental, why does it depend on contingent facts of human physiology? A phenomenological answer should not avoid the difficulty by appealing to vague wholeness.",
            "Response: the body schema is not a hidden picture of the body, but a name for the integrated orientation of potential action. Its explanatory value must be earned in careful cases—habit acquisition, altered motor capacities, tool use and breakdowns in coordinated action. The description is philosophical insofar as it challenges the assumption that meaningful space is assembled from self-contained sensations by reflective synthesis.",
            "Closing question: can the lived body be described without presupposing the world and the others through which its abilities are cultivated? Merleau-Ponty's account of bodily subjectivity is not an isolated bodily ego. It points toward the reversibility and ambiguity of embodied experience. An adequate debate must ask where such descriptions end and explanatory scientific theories begin."
        ))
    )
    fun lecturesFor(workId:String) = lectures.filter { it.workId == workId }

    val dossiers = listOf(
        CommentaryDossier("bt07",
            "How can phenomenology serve an ontological, rather than merely psychological, investigation?",
            "The methodological demand is to let what shows itself be seen from itself, while investigating the ways concealment and interpretation mediate that showing.",
            listOf("Distinguish the phenomenon from a mere appearance understood as an index of something absent.",
                   "Investigate how logos can function as discourse that makes what is discussed manifest.",
                   "Explain why the analysis of Dasein's everyday being-in-the-world is a methodological entry into the question of Being, not the whole answer."),
            "A Husserlian may insist that every such disclosure still requires a description of the intentional conditions of appearing.",
            "If the structure of disclosure is already interpreted through an existential analytic, can it genuinely present phenomena without presuppositions?",
            "The appropriate defense is methodological modesty: exhibit particular cases of concealment and disclosure and argue for the structural account rather than asserting that description is ever absolutely presuppositionless.",
            "Being and Time, Introduction §7. Check against the user's edition; no page-level citation asserted."),
        CommentaryDossier("id02",
            "Does bracketing suspend the world or abolish it?",
            "Epoché concerns the suspension of the unexamined natural-attitude thesis within inquiry, not a demonstration that the world does not exist.",
            listOf("Identify the natural attitude's ordinary existential commitment.",
                   "Describe the methodological suspension of using this commitment as a premise.",
                   "Explain how worldly objects may still be studied as intended and given in experience."),
            "An existentialist objection maintains that the philosopher cannot step outside pretheoretical worldly involvement.",
            "What warrants the transition from suspension to the thesis of transcendental subjectivity?",
            "The transition needs a separate argument; an instruction to bracket the world does not, by itself, prove all the claims of transcendental idealism.",
            "Ideas I, sections on the natural attitude and phenomenological reduction; edition/page unchecked."),
        CommentaryDossier("bt01",
            "Why reopen the question of Being if entities are already intelligible?",
            "Heidegger argues that our pre-ontological familiarity can mask, rather than settle, the question of the meaning of Being.",
            listOf("Separate acquaintance with particular entities from an articulated account of Being.",
                   "Identify the objections from universality, indefinability and self-evidence.",
                   "Reconstruct why the very intelligibility that makes the question seem trivial also makes it pressing."),
            "A critic can object that the proposed distinction lacks a determinate question until a criterion of a successful answer is stated.",
            "Does pre-ontological understanding supply enough structure to begin the analytic without begging the question?",
            "The opening is programmatic: it motivates the inquiry, whereas its method and success must be evaluated in subsequent divisions.",
            "Being and Time, Introduction §§1–2; edition/page unchecked.")
    )
    fun dossierFor(lessonId:String) = dossiers.firstOrNull { it.lessonId == lessonId }

    val lexemes = listOf(
        LexicalProblem("Sein","German","Substantivized infinitive of sein (to be)",
            listOf("Being","to-be / being as such"),
            "Names what is asked about in the question of Being. A capitalized English 'Being' is an editorial convention, not a separate German spelling rule.",
            "Translation proposals are explanatory possibilities, not quotations of a specific published edition."),
        LexicalProblem("Seiendes","German","Present participle of sein used substantively (that which is / what is)",
            listOf("entity / entities","being / beings","what is"),
            "Keeping the verbal kinship with Sein and the conceptual difference between Being and entities both matters; neither English option achieves both effortlessly.",
            "Context determines number and grammatical case; comparison with an authorized passage is necessary."),
        LexicalProblem("Lebenswelt","German","Compound: Leben (life) + Welt (world)",
            listOf("lifeworld","life-world"),
            "In later Husserl, the lifeworld is not simply a private worldview, but a horizon of familiar meaningful experience and pretheoretical evidence.",
            "Technical meaning shifts across contexts and authors."),
        LexicalProblem("epoché","Greek through phenomenological usage","Suspension or withholding of judgment; conventional Greek term appropriated by phenomenology",
            listOf("bracketing","suspension"),
            "Phenomenological epoché is a methodological change of attitude, not a claim that things cease to exist.",
            "The lexical history alone does not settle Husserl's transcendental argument."),
        LexicalProblem("Leib / Körper","German","Leib: lived/animated body; Körper: material body",
            listOf("lived body / physical body","living body / corporeal body"),
            "The distinction marks different ways of describing embodiment and is not an absolute separation of two substances.",
            "Translation contrasts depend on the author and specific passage.")
    )
    val protocols = listOf(
        LaboratoryProtocol("retention","Listening to a melody",
            "Attend to two consecutive sounds. Describe how the just-elapsed sound remains relevant while the current sound is heard, without supplying a theoretical explanation.",
            "Separate immediate temporal holding from an explicit act of remembering a past event.",
            "Consider a disconnected sequence of random sounds. Is the same retention structure still present?","time"),
        LaboratoryProtocol("tool","The ready-to-hand",
            "Describe using an everyday tool without first making it a detached theoretical object.",
            "Distinguish the equipment's practical availability from explicit judgments about its physical properties.",
            "Describe what changes when the tool suddenly breaks.","heidegger"),
        LaboratoryProtocol("body","Bodily orientation",
            "Reach toward an object and describe the sense of distance and possibility preceding reflective measurements.",
            "Distinguish experienced bodily capacity from a third-person description of your muscles.",
            "What if the object is beyond reach, or familiar action is interrupted?","merleau"),
        LaboratoryProtocol("empathy","Another person's expression",
            "Describe encountering another person's sadness without positing that their experience is directly yours.",
            "Distinguish perceiving expression from inferring invisible interior states.",
            "Could an actor's performance or misreading undermine the description?","stein")
    )
}
