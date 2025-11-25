package com.skill_forge.infy_intern.repository;

import com.skill_forge.infy_intern.model.QuizResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizResponseRepository extends MongoRepository<QuizResponse, String> {
}
