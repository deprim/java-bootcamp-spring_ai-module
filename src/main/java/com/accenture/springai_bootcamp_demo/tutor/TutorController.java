package com.accenture.springai_bootcamp_demo.tutor;

import com.accenture.springai_bootcamp_demo.tutor.TutorDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor")
@RequiredArgsConstructor
public class TutorController {
    private final TutorService tutor;

    @PostMapping("/ask")
    public Answer ask(@Valid @RequestBody AskRequest request) {
        return tutor.ask(request);
    }

    @PostMapping("/study-plan")
    public StudyPlan studyPlan(@Valid @RequestBody StudyPlanRequest request) {
        return tutor.studyPlan(request);
    }
}
