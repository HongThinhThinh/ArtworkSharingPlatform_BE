package start.service;

import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import start.dto.request.RechargeRequestDTO;
import start.dto.request.WithDrawRequestDTO;
import start.dto.response.TransactionResponseDTO;
import start.entity.Transaction;
import start.entity.User;
import start.entity.Wallet;
import start.enums.RoleEnum;
import start.enums.TransactionEnum;
import start.repository.TransactionRepository;
import start.repository.UserRepository;
import start.repository.WalletRepository;
import start.utils.AccountUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WalletService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AccountUtils accountUtils;


    @Autowired
    private PaypalService payPalService;


    @Autowired
  private   EmailService emailService;
    @Autowired
  private UserRepository userRepository;

      public String createUrl(RechargeRequestDTO rechargeRequestDTO) throws NoSuchAlgorithmException, InvalidKeyException, Exception{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime createDate = LocalDateTime.now();
        String formattedCreateDate = createDate.format(formatter);



        User user = accountUtils.getCurrentUser();

        String orderId = UUID.randomUUID().toString().substring(0,6);

        Wallet wallet = walletRepository.findWalletByUser_Id(user.getId());

        Transaction transaction = new Transaction();

        transaction.setAmount(Float.parseFloat(rechargeRequestDTO.getAmount()));
        transaction.setTransactionType(TransactionEnum.PENDING);
        transaction.setTo(wallet);
        transaction.setTransactionDate(formattedCreateDate);
        transaction.setDescription("Recharge");
        Transaction transactionReturn = transactionRepository.save(transaction);

        String tmnCode = "EDXJUBE1";
        String secretKey = "AYBLLQTVRSRCZCGJGXJQCMNMLIYKKILF";
        String vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String returnUrl = "http://mycremo.art/profile/wallet?id="+transactionReturn.getTransactionID();

        String currCode = "VND";
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_CurrCode", currCode);
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Thanh toan cho ma GD: " + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Amount", rechargeRequestDTO.getAmount() +"00");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_CreateDate", formattedCreateDate);
        vnpParams.put("vnp_IpAddr", "128.199.178.23");

        StringBuilder signDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            signDataBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
            signDataBuilder.append("=");
            signDataBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            signDataBuilder.append("&");
        }
        signDataBuilder.deleteCharAt(signDataBuilder.length() - 1); // Remove last '&'

        String signData = signDataBuilder.toString();
        String signed = generateHMAC(secretKey, signData);

        vnpParams.put("vnp_SecureHash", signed);

        StringBuilder urlBuilder = new StringBuilder(vnpUrl);
        urlBuilder.append("?");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            urlBuilder.append("&");
        }
        urlBuilder.deleteCharAt(urlBuilder.length() - 1); // Remove last '&'

        return urlBuilder.toString();
    }



    public String createPaypalPayment(RechargeRequestDTO rechargeRequestDTO) throws PayPalRESTException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime createDate = LocalDateTime.now();
        String formattedCreateDate = createDate.format(formatter);

        User user = accountUtils.getCurrentUser();

        Wallet wallet = walletRepository.findWalletByUser_Id(user.getId());

        Transaction transaction = new Transaction();
        transaction.setAmount(Float.parseFloat(rechargeRequestDTO.getAmount()));
        transaction.setTransactionType(TransactionEnum.PENDING);
        transaction.setTo(wallet);
        transaction.setDescription("Recharge");
        transaction.setTransactionDate(formattedCreateDate);
        Transaction transactionReturn = transactionRepository.save(transaction);

        Double totalAmount = Double.parseDouble(rechargeRequestDTO.getAmount());
        String currency = "USD";

        String cancelUrl = "http://yourwebsite.com/cancel";
        String successUrl ="";
User currentUser = accountUtils.getCurrentUser();

if(currentUser.getRole().equals(RoleEnum.AUDIENCE)) {
    successUrl= "http://mycremo.art/profile/wallet?id="+transactionReturn.getTransactionID();
}
if(currentUser.getRole().equals(RoleEnum.CREATOR)){
    successUrl= "http://mycremo.art/creator-manage/wallet?id="+transactionReturn.getTransactionID();
}

        Payment payment = payPalService.createPayment(
                totalAmount,
                currency,
                "Recharge",
                cancelUrl,
                successUrl);

        if (payment != null) {
            return payment.getLinks().get(1).getHref(); // Return the PayPal redirect URL
        } else {
            return null;
        }
    }


    private String generateHMAC(String secretKey, String signData) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSha512.init(keySpec);
        byte[] hmacBytes = hmacSha512.doFinal(signData.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : hmacBytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }


    public Wallet recharge(UUID id) {
        User user = accountUtils.getCurrentUser();
        Transaction transaction = transactionRepository.findByTransactionID(id);
        Wallet wallet = walletRepository.findWalletByUser_Id(user.getId());
        if(transaction.getTransactionType().equals(TransactionEnum.PENDING)) {
            if(wallet.getWalletID() == transaction.getTo().getWalletID()){
                wallet.setBalance(wallet.getBalance()+transaction.getAmount());
            }
        }
        else{
            throw new RuntimeException("Reload");
        }
        transaction.setTransactionType(TransactionEnum.RECHARGE);

        transactionRepository.save(transaction);
        return walletRepository.save(wallet);
    }

    public  Wallet walletDetail(UUID id) {
        return  walletRepository.findWalletByUser_Id(id);
    }

    public Transaction withDraw(WithDrawRequestDTO withDrawRequestDTO) {
        User user = accountUtils.getCurrentUser();
        Wallet wallet = walletRepository.findWalletByUser_Id(user.getId());
        if (wallet.getBalance() >= (withDrawRequestDTO.getAmount())) {
            Transaction transaction = new Transaction();
            transaction.setAmount((withDrawRequestDTO.getAmount()));
            transaction.setTransactionType(TransactionEnum.WITHDRAW_PENDING);
            transaction.setFrom(wallet);
            transaction.setDescription("WITHDRAW");
            transaction.setAccountName(withDrawRequestDTO.getAccountName());
            transaction.setBankName(withDrawRequestDTO.getBankName());
            transaction.setAccountNumber(withDrawRequestDTO.getAccountNumber());
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            transaction.setTransactionDate(now.format(formatter));
            wallet.setBalance(wallet.getBalance()-(withDrawRequestDTO.getAmount()));
            walletRepository.save(wallet);
            return transactionRepository.save(transaction);
        } else {
            throw new RuntimeException("Insufficient balance in wallet for withdrawal.");
        }
    }

    public List<TransactionResponseDTO> requestWithDraw() {
        List<TransactionResponseDTO> listTransactionResponseDTO = new ArrayList<>();
         List<Transaction> transactions = transactionRepository.findTransactionByTransactionType(TransactionEnum.WITHDRAW_PENDING);
        for (Transaction transaction : transactions) {
            TransactionResponseDTO transactionResponseDTO = new TransactionResponseDTO();
            transactionResponseDTO.setTransactionID(transaction.getTransactionID());
            transactionResponseDTO.setTransactionType(transaction.getTransactionType());
            transactionResponseDTO.setAmount(transaction.getAmount());
            transactionResponseDTO.setDescription(transaction.getDescription());
            transactionResponseDTO.setTransactionDate(transaction.getTransactionDate());
            transactionResponseDTO.setFrom(transaction.getFrom());
            transactionResponseDTO.setTo(transaction.getTo());
            if(transaction.getFrom() != null){
                transactionResponseDTO.setUserFrom(transaction.getFrom().getUser());
            }
            if(transaction.getTo() != null){
                transactionResponseDTO.setUserTo(transaction.getTo().getUser());
            }
            listTransactionResponseDTO.add(transactionResponseDTO);
        }
        return  listTransactionResponseDTO;
      }

    public Transaction acpWithDraw(UUID id) {
        Transaction transaction = transactionRepository.findByTransactionID(id);
        if (transaction != null) {
            transaction.setTransactionType(TransactionEnum.WITHDRAW_SUCCESS);
            threadSendMail(transaction.getFrom().getUser(), "Withdrawal Successfully", "Thank you for trusting and using Cremo");
            transactionRepository.save(transaction);
            return  transaction;
        }
        else{
            return null;
        }

    }


    public Transaction rejectWithDraw(UUID id, String reason) {
        Transaction transaction = transactionRepository.findByTransactionID(id);
        if (transaction != null) {
            Wallet wallet = transaction.getFrom();
            wallet.setBalance(wallet.getBalance()+ transaction.getAmount());
            transaction.setTransactionType(TransactionEnum.WITHDRAW_REJECT);
            transaction.setReasonWithdrawReject(reason);
            threadSendMail(transaction.getFrom().getUser(), "Withdrawal failed", "You Cannot Withdraw Because: " + reason);
            return transactionRepository.save(transaction);
        } else {
            return null;
        }
    }

    public void threadSendMail(User user,String subject, String description){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                emailService.sendMail(user,subject,description);
            }

        };
        new Thread(r).start();
    }
}
