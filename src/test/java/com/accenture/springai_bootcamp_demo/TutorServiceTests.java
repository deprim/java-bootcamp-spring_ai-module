package com.accenture.springai_bootcamp_demo;

import com.accenture.springai_bootcamp_demo.client.AiModelException;
import com.accenture.springai_bootcamp_demo.client.OllamaChatClient;
import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeDocument;
import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeRepository;
import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeService;
import com.accenture.springai_bootcamp_demo.tutor.TutorDtos.*;
import com.accenture.springai_bootcamp_demo.tutor.TutorService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TutorServiceTests {
    private OllamaChatClient model;
    private TutorService tutor;

    @BeforeEach
    void setup() {
        model = mock(OllamaChatClient.class);
        var repository = mock(KnowledgeRepository.class);
        when(repository.findAllByOrderByCreatedAtAscIdAsc()).thenReturn(List.of(
                KnowledgeDocument.create("Java records", "Java records are concise data carriers with generated accessors.")));
        tutor = new TutorService(new KnowledgeService(repository), model);
    }

    @Test
    void groundedAnswerIncludesActualRetrievedEvidenceAndCitationInstructions() {
        when(model.generate(anyString(), anyString())).thenReturn("Records carry data.");
        var result = tutor.ask(new AskRequest("What are Java records?"));
        assertThat(result.answer()).isEqualTo("Records carry data.");
        assertThat(result.sources()).hasSize(1);
        var system = ArgumentCaptor.forClass(String.class);
        var prompt = ArgumentCaptor.forClass(String.class);
        verify(model).generate(system.capture(), prompt.capture());
        assertThat(system.getValue()).containsIgnoringCase("cite").containsIgnoringCase("reference");
        assertThat(prompt.getValue()).contains("What are Java records?", "generated accessors", result.sources().getFirst().id());
    }

    @Test
    void missingEvidenceIsExplicitAndDoesNotCallModel() {
        var result = tutor.ask(new AskRequest("Unfindablexzpq?"));
        assertThat(result.sources()).isEmpty();
        assertThat(result.answer()).containsIgnoringCase("no matching");
        verifyNoInteractions(model);
    }

    @Test
    void researcherTutorAndReviewerMakeSeparateOrderedCallsWithHandoffs() {
        when(model.generate(anyString(), anyString()))
                .thenReturn("Research: records have accessors.", "Draft: practice records on day one.", "Reviewed: build a Student record.");
        var result = tutor.studyPlan(new StudyPlanRequest("Java records", Level.BEGINNER, 2));
        assertThat(result.plan()).isEqualTo("Reviewed: build a Student record.");
        assertThat(result.stages()).extracting(AgentStage::agent).containsExactly("researcher", "tutor", "reviewer");
        assertThat(result.stages()).extracting(AgentStage::output)
                .containsExactly("Research: records have accessors.", "Draft: practice records on day one.", "Reviewed: build a Student record.");
        var systems = ArgumentCaptor.forClass(String.class);
        var prompts = ArgumentCaptor.forClass(String.class);
        verify(model, times(3)).generate(systems.capture(), prompts.capture());
        assertThat(systems.getAllValues().get(0)).containsIgnoringCase("researcher");
        assertThat(systems.getAllValues().get(1)).containsIgnoringCase("tutor");
        assertThat(systems.getAllValues().get(2)).containsIgnoringCase("reviewer");
        assertThat(prompts.getAllValues().get(0)).contains("generated accessors");
        assertThat(prompts.getAllValues().get(1)).contains("Research: records have accessors.", "BEGINNER", "2");
        assertThat(prompts.getAllValues().get(2)).contains("Draft: practice records on day one.", "Research: records have accessors.");
    }

    @Test
    void stageFailureStopsTheWorkflowInsteadOfReturningAPartialPlan() {
        when(model.generate(anyString(), anyString())).thenReturn("Research notes")
                .thenThrow(new AiModelException("Local model unavailable"));
        assertThatThrownBy(() -> tutor.studyPlan(new StudyPlanRequest("Java records", Level.BEGINNER, 1)))
                .isInstanceOf(AiModelException.class);
        verify(model, times(2)).generate(anyString(), anyString());
    }
}
