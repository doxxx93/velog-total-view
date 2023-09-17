package io.velog.statistic.total;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/total")
public class TotalController {

    private final TotalService totalService;

    public TotalController(TotalService totalService) {
        this.totalService = totalService;
    }

    @PostMapping
    public Mono<Integer> getTotal(@RequestBody TotalRequest request) {
        return totalService.getTotal(request);
    }
}

