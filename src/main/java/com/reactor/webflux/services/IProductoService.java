package com.reactor.webflux.services;

import com.reactor.webflux.models.Categoria;
import com.reactor.webflux.models.Producto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IProductoService {

    Flux<Producto> findAll();

    Flux<Producto> findAllUpperCase();

    Flux<Producto> findAllUpperCaseRepeat();

    Mono<Producto> findById(String id);

    Mono<Producto> save(Producto producto);

    Mono<Void> delete(Producto producto);

    Flux<Categoria> findAllCategoria();

    Mono<Categoria> findCategoriaById(String id);

    Mono<Categoria> saveCategoria(Categoria categoria);
}
