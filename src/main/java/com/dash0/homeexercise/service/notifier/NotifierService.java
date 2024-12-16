package com.dash0.homeexercise.service.notifier;

import com.dash0.homeexercise.service.LogAttributeCounterService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotifierService {
	private static final Logger LOGGER = LoggerFactory.getLogger(NotifierService.class);

	@Autowired
	private LogAttributeCounterService logAttributeCounterService;

	@Autowired
	private Notifier notifier;

	@Scheduled(fixedRateString = "${grpc.logs.window}")
	public void execute() {
		LOGGER.debug("Notify triggered");
		final var state = logAttributeCounterService.observeAndReset();
		notifier.notifyValues(state);
	}
}
