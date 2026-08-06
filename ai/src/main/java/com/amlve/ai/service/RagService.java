package com.amlve.ai.service;

import reactor.core.publisher.Flux;

public interface RagService {


    Flux<String> answer(String message);
}
