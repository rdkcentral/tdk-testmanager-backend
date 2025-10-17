package com.rdkm.tdkci.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.rdkm.tdkci.response.DataResponse;
import com.rdkm.tdkci.response.Response;

/**
 * Global exception handler for handling various exceptions across the
 * application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());

	/**
	 * This method is used to handle the validation exceptions and return the error
	 * response with the field name and error message in the response body for
	 * controller methods with @Valid annotation
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ResponseEntity<DataResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
		LOGGER.info("Validation failed: " + ex.getMessage());
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		DataResponse errorResponse = new DataResponse("Invalid User inputs in request", HttpStatus.BAD_REQUEST.value(),
				errors);
		return ResponseEntity.badRequest().body(errorResponse);
	}

	/**
	 * This method is used to handle the ResourceAlreadyExistsException and return
	 * the error response with the error message in the response body and HTTP
	 * status code 409
	 * 
	 * @param exception
	 * @return ResponseEntity
	 */
	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<Response> handleResourceAlreadyRegistered(ResourceAlreadyExistsException ex) {
		Response error = new Response(ex.getMessage(), HttpStatus.CONFLICT.value());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	/**
	 * This method is used to handle the ResourceNotFoundException and return the
	 * error response with the error message in the response body
	 * 
	 * @param ex
	 * @return ResponseEntity
	 */

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Response> handleResourceNotExists(ResourceNotFoundException ex) {
		Response error = new Response(ex.getMessage(), HttpStatus.NOT_FOUND.value());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * This method is used to handle the BadCredentialsException and return the
	 * error response with the error message in the response body
	 * 
	 * @return ResponseEntity
	 * 
	 */
	@ExceptionHandler(TDKCIServiceException.class)
	public ResponseEntity<Response> handleTDKServiceException(TDKCIServiceException ex) {
		Response errorResponse = new Response(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	/**
	 * This method is used to handle the UserInputException and return the error
	 * response with the error message in the response body
	 * 
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(UserInputException.class)
	public ResponseEntity<Response> handleUserInputException(UserInputException ex) {
		Response errorResponse = new Response(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

	}

	/**
	 * This method is used to handle the DeleteFailedException and return the error
	 * response with the error message in the response body
	 * 
	 * @param ex
	 * @return
	 */
	@ExceptionHandler(DeleteFailedException.class)
	public ResponseEntity<Response> handleDeleteFailedException(DeleteFailedException ex) {
		Response errorResponse = new Response(ex.getMessage(), HttpStatus.CONFLICT.value());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
	}

}
