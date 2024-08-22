package com.dg.ServerRebornFarmguard;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@SpringBootApplication

public class ServerSpringRebornFarmguard {




	public static void main(String[] args) {
		SpringApplication.run(ServerSpringRebornFarmguard.class, args);

	}

	@Scheduled(fixedRate = 2000)
	public void so(){
		System.out.println("WORK");
	}




}
