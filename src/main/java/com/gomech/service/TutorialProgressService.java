package com.gomech.service;

import com.gomech.dto.Tutorial.TutorialProgressResponseDTO;
import com.gomech.model.TutorialProgress;
import com.gomech.model.User;
import com.gomech.repository.TutorialProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class TutorialProgressService {

    private static final Logger logger = LoggerFactory.getLogger(TutorialProgressService.class);

    private final TutorialProgressRepository tutorialProgressRepository;

    public TutorialProgressService(TutorialProgressRepository tutorialProgressRepository) {
        this.tutorialProgressRepository = tutorialProgressRepository;
    }

    @Transactional
    public TutorialProgressResponseDTO getProgress(User user) {
        logger.info("Buscando progresso de tutoriais para usuário: {}", user.getId());
        
        TutorialProgress progress = tutorialProgressRepository.findByUser(user)
            .orElseGet(() -> {
                logger.info("Nenhum progresso encontrado, criando e salvando novo registro para usuário: {}", user.getId());
                TutorialProgress newProgress = new TutorialProgress(user);
                return tutorialProgressRepository.save(newProgress);
            });

        return TutorialProgressResponseDTO.fromEntity(progress);
    }

    @Transactional
    public TutorialProgressResponseDTO markAsViewed(User user, String tutorialKey) {
        logger.info("Marcando tutorial '{}' como visualizado para usuário: {}", tutorialKey, user.getId());

        TutorialProgress progress = tutorialProgressRepository.findByUser(user)
            .orElseGet(() -> {
                logger.info("Criando novo registro de progresso para usuário: {}", user.getId());
                return new TutorialProgress(user);
            });

        progress.markTutorialAsCompleted(tutorialKey);
        TutorialProgress saved = tutorialProgressRepository.save(progress);

        logger.info("Tutorial '{}' marcado como visualizado para usuário: {}", tutorialKey, user.getId());
        return TutorialProgressResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public boolean hasTutorialCompleted(User user, String tutorialKey) {
        return tutorialProgressRepository.findByUser(user)
            .map(progress -> progress.hasTutorialCompleted(tutorialKey))
            .orElse(false);
    }
}

