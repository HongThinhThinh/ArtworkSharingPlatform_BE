package start.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import start.dto.response.TransactionResponseDTO;
import start.entity.Transaction;
import start.service.TransactionService;
import start.utils.ResponseHandler;

import java.util.List;

@RestController
@SecurityRequirement(name ="api")
@CrossOrigin("*")
public class TransactionController {

    @Autowired
    ResponseHandler responseHandler;

    @Autowired
    TransactionService transactionService;

    @GetMapping("/transactionsById")
    public ResponseEntity getTransactionById(){
        List<TransactionResponseDTO> transaction = transactionService.getTransactionById();
        return  responseHandler.response(200, "Get Transaction  Successfully!", transaction);
    }

    @GetMapping("/allTransactions")
    public ResponseEntity transactions(){
        List<TransactionResponseDTO> transaction = transactionService.allTransactions();
        return  responseHandler.response(200, "Get All Transaction  Successfully!", transaction);
    }

    @GetMapping("/historyBuyArtworkById")
    public ResponseEntity historyBuyArtworkById(){
        List<TransactionResponseDTO> transaction = transactionService.historyBuyArtworkById();
        return  responseHandler.response(200, "Get All History Buy Artwork  Successfully!", transaction);
    }
}
