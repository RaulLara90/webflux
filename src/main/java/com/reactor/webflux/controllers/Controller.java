package com.reactor.webflux.controllers;

import com.reactor.webflux.models.Categoria;
import com.reactor.webflux.models.Producto;
import com.reactor.webflux.services.IProductoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@org.springframework.stereotype.Controller
@SessionAttributes("producto")
public class Controller {

    @Autowired
    private IProductoService productoService;
    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    @Value("${resources}")
    private String resource;

    @ModelAttribute("categorias")
    public Flux<Categoria> categorias() {
        return productoService.findAllCategoria();
    }

    @GetMapping("/uploads/img/{nombreFoto:.+}")
    public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
        Path route = Paths.get(resource).resolve(nombreFoto).toAbsolutePath();
        Resource image = new UrlResource(route.toUri());

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filaname=\"" + image.getFilename() + "\"")
                .body(image));
    }

    @GetMapping({"/listar", "/"})
    public String listar(Model model) {

        Flux<Producto> productos = productoService.findAllUpperCase();

        productos.subscribe(prod -> log.info(prod.getNombre()));
        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/ver/{id}")
    public Mono<String> ver(Model model, @PathVariable String id) {
        return productoService.findById(id).doOnNext(p -> {
            model.addAttribute("producto", p);
            model.addAttribute("titulo", "Detalle del producto");
        }).switchIfEmpty(Mono.just(new Producto()))
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                }).then(Mono.just("ver"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @GetMapping("/form")
    public Mono<String> crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de producto");
        model.addAttribute("boton", "Crear");
        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model) {
        Mono<Producto> productoMono = productoService.findById(id)
                .doOnNext(p -> log.info(p.getNombre()))
                .defaultIfEmpty(new Producto());
        model.addAttribute("titulo", "Editar producto");
        model.addAttribute("boton", "Editar");
        model.addAttribute("producto", productoMono);
        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono<String> editarV2(@PathVariable String id, Model model) {
        return productoService.findById(id)
                .doOnNext(p -> {
                    log.info(p.getNombre());
                    model.addAttribute("titulo", "Editar producto");
                    model.addAttribute("producto", p);
                    model.addAttribute("boton", "Editar");
                }).defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @PostMapping("/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult result, Model model, @RequestPart FilePart file, SessionStatus status) {

        if (result.hasErrors()) {
            model.addAttribute("titulo", "Errores en formulario producto");
            model.addAttribute("boton", "Guardar");
            return Mono.just("form");
        } else {
            status.setComplete();
            Mono<Categoria> categoria = productoService.findCategoriaById(producto.getCategoria().getId());
            return categoria.flatMap(c -> {
                if (producto.getCreateAt() == null) {
                    producto.setCreateAt(new Date());
                }
                if (!file.filename().isEmpty()) {
                    producto.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                            .replace(StringUtils.SPACE, StringUtils.EMPTY)
                            .replace(":", StringUtils.EMPTY)
                            .replace("\\", StringUtils.EMPTY));
                }
                producto.setCategoria(c);
                return productoService.save(producto);
            }).doOnNext(p -> {
                log.info("Categoria asginada: " + p.getCategoria().getNombre() + " Id: " + p.getCategoria().getId());
                log.info("Productor guardado: " + p.getNombre() + " Id: " + p.getId());
            })
                    .flatMap(p -> {
                        if (!file.filename().isEmpty()) {
                            return file.transferTo(new File(resource + p.getFoto()));
                        }
                        return Mono.empty();
                    })
                    .thenReturn("redirect:/listar?success=producto+guardado+con+exito");
        }
    }

    @GetMapping("/eliminar/{id}")
    public Mono<String> eliminar(@PathVariable String id) {
        return productoService.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto a eliminar!"));
                    }
                    return Mono.just(p);
                })
                .flatMap(productoService::delete)
                .then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto+a+eliminar"));
    }

    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model) {

        Flux<Producto> productos = productoService.findAllUpperCase().delayElements(Duration.ofSeconds(1));

        productos.subscribe(prod -> log.info(prod.getNombre()));
        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-full")
    public String listarFull(Model model) {

        Flux<Producto> productos = productoService.findAllUpperCaseRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-chunked")
    public String listarChunked(Model model) {

        Flux<Producto> productos = productoService.findAllUpperCaseRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listarChunked";
    }


}
