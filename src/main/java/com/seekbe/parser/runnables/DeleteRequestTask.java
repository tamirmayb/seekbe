package com.seekbe.parser.runnables;

import com.seekbe.parser.SBController;
import com.seekbe.parser.repositories.RequestRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@AllArgsConstructor
public class DeleteRequestTask implements Runnable {
    private static Logger logger = LogManager.getLogger(SBController.class);

    private RequestRepository requestRepository;
    private String requestId;

    @Override
    public void run() {
        requestRepository.deleteById(requestId);
        logger.info("done delete " + requestId);
    }
}
