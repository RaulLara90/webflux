package com.reactor.webflux.repository;

import com.reactor.webflux.models.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends ReactiveMongoRepository<Categoria, String> {
}
