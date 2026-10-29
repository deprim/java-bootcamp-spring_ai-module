package com.accenture.springai_bootcamp_demo.tutor;

import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeDtos.Source;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class TutorDtos {
    private TutorDtos() { }

    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED }

    public record AskRequest(@NotBlank @Size(max = 2000) String question) { }
    public record Answer(String answer, List<Source> sources) { }

    public record StudyPlanRequest(@NotBlank @Size(max = 300) String topic,
                                   @NotNull Level level, @Min(1) @Max(7) int days) { }
    public record AgentStage(String agent, String output) { }
    public record StudyPlan(String plan, List<AgentStage> stages, List<Source> sources) { }
}
