package com.savvato.tribeapp.controllers;

import com.savvato.tribeapp.controllers.annotations.controllers.ConnectAPIController.*;
import com.savvato.tribeapp.controllers.annotations.responses.BadRequest;
import com.savvato.tribeapp.controllers.dto.ConnectRequest;
import com.savvato.tribeapp.controllers.dto.CosignRequest;
import com.savvato.tribeapp.dto.*;
import com.savvato.tribeapp.services.ConnectService;
import com.savvato.tribeapp.services.CosignService;
import com.savvato.tribeapp.services.GenericResponseService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "connect", description = "Connections between users")
@RequestMapping("/api/connect")
public class ConnectAPIController {
  @Autowired ConnectService connectService;

  @Autowired
  CosignService cosignService;

  @Autowired
  GenericResponseService genericResponseService;

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
    log.error("Exception occurred: " + ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error: " + ex.getMessage());
  }

  ConnectAPIController() {}

  @GetConnections
  @GetMapping("/{userId}/all")
  public ResponseEntity<List<ConnectOutgoingMessageDTO>> getConnections(
      @Parameter(description = "The user ID of a user", example = "1") @PathVariable Long userId) {

    List<ConnectOutgoingMessageDTO> list = connectService.getAllConnectionsForAUser(userId);

    if (list != null) {
      return ResponseEntity.status(HttpStatus.OK).body(list);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
  }

  @GetQRCodeString
  @GetMapping("/{userId}")
  public ResponseEntity getQrCodeString(
      @Parameter(description = "The user ID of a user", example = "1") @PathVariable Long userId) {

    Optional<String> opt = connectService.storeQRCodeString(userId);

    if (opt.isPresent()) {
      return ResponseEntity.status(HttpStatus.OK).body(opt.get());
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }
  }

  @Connect
  @PostMapping
  public ResponseEntity<GenericResponseDTO> connect(@RequestBody @Valid ConnectRequest connectRequest) {
    GenericResponseDTO connection = connectService.connect(connectRequest);

    if(connection.booleanMessage) {
      return ResponseEntity.status(HttpStatus.OK).body(connection);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(connection);
    }
  }

  @MessageMapping("/connect/room")
  public void connect(@Payload ConnectIncomingMessageDTO incoming, @Header("simpSessionId") String sessionId) {
      connectService.connect(incoming);
  }

  @SaveCosign
  @PostMapping("/cosign")
  public ResponseEntity saveCosign(@RequestBody @Valid CosignRequest cosignRequest) {

    Optional opt = cosignService.cosign(cosignRequest);

    if(opt.get() instanceof GenericResponseDTO) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(opt.get());
    }

    return ResponseEntity.status(HttpStatus.OK).body(opt.get());
  }
  @DeleteCosign
  @DeleteMapping("/cosign")
  public ResponseEntity deleteCosign(@RequestBody @Valid CosignRequest cosignRequest) throws Exception {

    cosignService.deleteCosign(cosignRequest.userIdIssuing, cosignRequest.userIdReceiving, cosignRequest.phraseId);

    return ResponseEntity.status(HttpStatus.OK).build();

  }

  @GetCosignersForUserAttribute
  @GetMapping("cosign/{userIdReceiving}/{phraseId}")
  public ResponseEntity<List<UsernameDTO>> getCosignersForUserAttribute(@PathVariable Long userIdReceiving, @PathVariable Long phraseId) {

    List<UsernameDTO> list = cosignService.getCosignersForUserAttribute(userIdReceiving,phraseId);

    return ResponseEntity.status(HttpStatus.OK).body(list);
  }

  @GetAllCosignsForUser
  @GetMapping("cosign/{userIdReceiving}/all")
  public ResponseEntity<List<CosignsForUserDTO>> getAllCosignsForUser(@PathVariable Long userIdReceiving) {

    List<CosignsForUserDTO> list = cosignService.getAllCosignsForUser(userIdReceiving);

    return ResponseEntity.status(HttpStatus.OK).body(list);
  }

}
