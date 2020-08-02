package com.reactor.webflux.services;

import com.reactor.webflux.models.Categoria;
import com.reactor.webflux.models.Producto;
import com.reactor.webflux.repository.CategoriaRepository;
import com.reactor.webflux.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductoServiceImpl implements IProductoService {

    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    @Override
    public Flux<Producto> findAll() {
        return productoRepository.findAll();
    }

    @Override
    public Flux<Producto> findAllUpperCase() {
        return productoRepository.findAll().map(producto -> {
            producto.setNombre(producto.getNombre().toUpperCase());
            return producto;
        }).doOnNext(prod -> log.info(prod.getNombre()));
    }

    @Override
    public Flux<Producto> findAllUpperCaseRepeat() {
        return findAllUpperCase().repeat(5000);
    }


    @Override
    public Mono<Producto> findById(String id) {
        return productoRepository.findById(id);
    }

    @Override
    public Mono<Producto> save(Producto producto) {
        return productoRepository.save(producto);
    }

    @Override
    public Mono<Void> delete(Producto producto) {
        return productoRepository.delete(producto);
    }

    @Override
    public Flux<Categoria> findAllCategoria() {
        return categoriaRepository.findAll();
    }

    @Override
    public Mono<Categoria> findCategoriaById(String id) {
        return categoriaRepository.findById(id);
    }

    @Override
    public Mono<Categoria> saveCategoria(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }
}
