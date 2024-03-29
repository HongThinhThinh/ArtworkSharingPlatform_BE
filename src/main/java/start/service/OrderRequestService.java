package start.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import start.dto.request.DemoOrderRequestDTO;
import start.dto.request.OrderRequestDTO;
import start.dto.response.OrderResponseDTO;
import start.entity.DemoRequest;
import start.entity.OrderRequest;
import start.entity.User;
import start.enums.RoleEnum;
import start.enums.StatusEnum;
import start.exception.exceptions.CannotOrderYourself;
import start.exception.exceptions.RefuseAuthorizeOrder;
import start.repository.DemoOrderRepository;
import start.repository.OrderRequestRepository;
import start.repository.SystemProfitRepository;
import start.repository.UserRepository;
import start.utils.AccountUtils;

import java.util.List;
import java.util.Set;

@Service
public class OrderRequestService {

    @Autowired
    OrderRequestRepository orderRequestRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AccountUtils accountUtils;

    @Autowired
    EmailService emailService;

    @Autowired
    DemoOrderRepository demoOrderRepository;

    @Autowired
    SystemProfitRepository systemProfitRepository;



    public OrderRequest sendOrderRequest(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = new OrderRequest();
        User creator = new User();
        creator = userRepository.findUserById(orderRequestDTO.getUserID());
        User audience = accountUtils.getCurrentUser();
        if(!creator.getId().equals(audience.getId())){
                orderRequest.setCreator(creator);
                orderRequest.setAudience(audience);
                orderRequest.setTitle(orderRequestDTO.getTitle());
                orderRequest.setDescription(orderRequestDTO.getDescription());
                orderRequest.setDateStart(orderRequestDTO.getDateStart());
                orderRequest.setDateEnd(orderRequestDTO.getDateEnd());
                orderRequest.setPrice(orderRequestDTO.getPrice());
                orderRequest.setStatus(StatusEnum.PENDING);
                threadSendMail(creator,"Order Artwork","You receive an order artwork");
        }else{
            throw new CannotOrderYourself("You may not order yourself");
        }

        return orderRequestRepository.save(orderRequest);
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

    public OrderRequest updateOrderRequestCreator(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(orderRequestDTO.getId());
        float cocMoney = orderRequestDTO.getPrice()/2;
        User creator  = orderRequest.getCreator();
        if(orderRequestDTO.getStatus().toLowerCase().trim().equals("active")){
            if(creator.getWallet().getBalance() >= cocMoney){
                creator.getWallet().setBalance(creator.getWallet().getBalance()-cocMoney);
                creator.getWallet().setCocMoney(creator.getWallet().getCocMoney()+cocMoney);
                orderRequest.setDateEnd(orderRequestDTO.getDateEnd());
                orderRequest.setPrice(orderRequestDTO.getPrice());
                orderRequest.setStatus(StatusEnum.ACTIVE);
                threadSendMail(orderRequest.getAudience(),"Order Artwork " + orderRequest.getTitle()+ " Success","Thank you for trusting us to use cremo");
            }else{
                throw new RuntimeException("Wallet you don't have enough 50% of the order value");
            }
        }else{
            orderRequest.setStatus(StatusEnum.REJECT);
            orderRequest.setReasonRejectCreator(orderRequestDTO.getReasonRejectCreator());
            threadSendMail(orderRequest.getAudience(),"Order Artwork " + orderRequest.getTitle()+ " Fail","Creator Cancel With Reason: " +orderRequest.getReasonRejectCreator());
        }
        userRepository.save(creator);
        return orderRequestRepository.save(orderRequest);
    }

    public OrderRequest updateOrderRequestAudience(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(orderRequestDTO.getId());
        float cocMoney = orderRequest.getPrice()/2;
        User audience = orderRequest.getAudience();
        User creator = orderRequest.getCreator();
        if(orderRequestDTO.getStatus().toLowerCase().trim().equals("processing")){
            if(audience.getWallet().getBalance() >= orderRequest.getPrice()){
                audience.getWallet().setBalance(audience.getWallet().getBalance()-orderRequest.getPrice());
                audience.getWallet().setCocMoney(audience.getWallet().getCocMoney()+orderRequest.getPrice());
                orderRequest.setStatus(StatusEnum.PROCESSING);
                threadSendMail(orderRequest.getCreator(),"Order Artwork " + orderRequest.getTitle()+ " Success","Thank you for trusting us to use cremo");
            }else{
                throw new RuntimeException("Wallet you don't have enough 100% of the order value");
            }
        }else{
            creator.getWallet().setBalance(creator.getWallet().getBalance()+cocMoney);
            creator.getWallet().setCocMoney(creator.getWallet().getCocMoney()-cocMoney);
            orderRequest.setStatus(StatusEnum.REJECT);
            orderRequest.setReasonRejectAudience(orderRequestDTO.getReasonRejectAudience());
            threadSendMail(orderRequest.getCreator(),"Order Artwork " + orderRequest.getTitle()+ " Fail","Audience Cancel With Reason: " +orderRequest.getReasonRejectAudience());
        }
        userRepository.save(audience);
        userRepository.save(creator);
        return orderRequestRepository.save(orderRequest);
    }

    public OrderRequest rejectOrderRequestAudience(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(orderRequestDTO.getId());
        orderRequest.setStatus(StatusEnum.REJECT);
        orderRequest.setReasonRejectCreator(orderRequestDTO.getReasonRejectAudience());
        return orderRequestRepository.save(orderRequest);
    }


    public List<OrderRequest> getOrderRequestCreatorStatus(StatusEnum status) {
        User user = accountUtils.getCurrentUser();
        List<OrderRequest> listOrderRequest = orderRequestRepository.findByCreatorIdAndStatus(user.getId(),status);
        return listOrderRequest;
    }

    public List<OrderRequest> getOrderRequestAudienceStatus(StatusEnum status) {
        User user = accountUtils.getCurrentUser();
        List<OrderRequest> listOrderRequest = orderRequestRepository.findByAudienceIdAndStatus(user.getId(),status);
        return listOrderRequest;
    }

    public OrderResponseDTO getOrderRequestDetail(long id) {
        OrderResponseDTO orderResponseDTO = new OrderResponseDTO();
        try{
           OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(id);
           orderResponseDTO.setId(orderRequest.getId());
           orderResponseDTO.setAudience(orderRequest.getAudience());
           orderResponseDTO.setCreator(orderRequest.getCreator());
           orderResponseDTO.setTitle(orderRequest.getTitle());
           orderResponseDTO.setDescription(orderRequest.getDescription());
           orderResponseDTO.setPrice(orderRequest.getPrice());
           orderResponseDTO.setDateStart(orderRequest.getDateStart());
           orderResponseDTO.setDateEnd(orderRequest.getDateEnd());
           orderResponseDTO.setReasonRejectAudience(orderRequest.getReasonRejectAudience());
           orderResponseDTO.setReasonRejectCreator(orderRequest.getReasonRejectCreator());
           orderResponseDTO.setDemoRequests(orderRequest.getDemoRequests());
           orderResponseDTO.setProductImage(orderRequest.getProductImage());
           orderResponseDTO.setProductMessage(orderRequest.getProductMessage());
           orderResponseDTO.setStatus(orderRequest.getStatus());
       }catch (Exception e){
            e.printStackTrace();
       }


        return orderResponseDTO;
    }

    public List<OrderRequest> getAllOrderRequestAudience() {
        User user = accountUtils.getCurrentUser();
        List<OrderRequest> listOrderRequest = orderRequestRepository.findByAudienceId(user.getId());
        return listOrderRequest;
    }

    public List<OrderRequest> getAllOrderRequestCreator() {
        User user = accountUtils.getCurrentUser();
        List<OrderRequest> listOrderRequest = orderRequestRepository.findByCreatorId(user.getId());
        return listOrderRequest;
    }

    public OrderRequest demoOrdeRequest(DemoOrderRequestDTO demoOrderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(demoOrderRequestDTO.getOrderId());
        DemoRequest demoRequest = new DemoRequest();
        demoRequest.setImage(demoOrderRequestDTO.getImage());
        demoRequest.setComment(demoOrderRequestDTO.getComment());
        demoRequest.setDescription(demoOrderRequestDTO.getDescription());
        demoRequest.setOrderRequest(orderRequest);
        demoOrderRepository.save(demoRequest);
        OrderRequest orderRequest1 = orderRequestRepository.findOrderRequestById(demoOrderRequestDTO.getOrderId());
        return orderRequest1;

    }

    public OrderRequest sendProduct(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(orderRequestDTO.getId());
        User audience = userRepository.findUserById(orderRequestDTO.getUserID());
        User creator = orderRequest.getCreator();
        float cocMoney = orderRequest.getPrice()/2;
        orderRequest.setProductImage(orderRequestDTO.getProductImage());
        orderRequest.setProductMessage(orderRequestDTO.getProductMessage());
        orderRequest.setStatus(StatusEnum.DONE);
        audience.getWallet().setCocMoney(audience.getWallet().getCocMoney()-orderRequest.getPrice());
        creator.getWallet().setCocMoney(creator.getWallet().getCocMoney()-cocMoney);
        creator.getWallet().setBalance(creator.getWallet().getBalance()+cocMoney);
        creator.getWallet().setBalance(creator.getWallet().getBalance()+orderRequest.getPrice());
        userRepository.save(audience);
        userRepository.save(creator);
        threadSendMail(audience,orderRequest.getTitle()+" Done",""+orderRequestDTO.getProductMessage()+"\n"+orderRequestDTO.getProductImage());
        return orderRequestRepository.save(orderRequest);
    }

    public OrderRequest sendOrderRequestGlobal(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = new OrderRequest();
        User audience = accountUtils.getCurrentUser();
        orderRequest.setAudience(audience);
        orderRequest.setTitle(orderRequestDTO.getTitle());
        orderRequest.setDescription(orderRequestDTO.getDescription());
        orderRequest.setDateStart(orderRequestDTO.getDateStart());
        orderRequest.setDateEnd(orderRequestDTO.getDateEnd());
        orderRequest.setPrice(orderRequestDTO.getPrice());
        orderRequest.setStatus(StatusEnum.GLOBAL);
        return orderRequestRepository.save(orderRequest);
    }

    public OrderRequest updateOrderRequestGlobal(OrderRequestDTO orderRequestDTO) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(orderRequestDTO.getId());
        User creator = accountUtils.getCurrentUser();
          if(creator.getRole().equals(RoleEnum.CREATOR)){
              orderRequest.setCreator(creator);
              orderRequest.setStatus(StatusEnum.PROCESSING);
              threadSendMail(orderRequest.getAudience(),"Order Global Artwork " + orderRequest.getTitle()+ " Success","Thank you for trusting us to use cremo");
          }else{
              throw  new RefuseAuthorizeOrder("You dont have the right to accept this order");
      }
        return orderRequestRepository.save(orderRequest);
    }

    public List<OrderRequest> getAllOrderRequestGlobal(StatusEnum statusEnum) {
        List<OrderRequest> listOrderRequest = orderRequestRepository.findByStatus(statusEnum);
       return listOrderRequest;

    }

    public OrderRequest cancelOrderRequest(long id) {
        OrderRequest orderRequest = orderRequestRepository.findOrderRequestById(id);
        User audience = accountUtils.getCurrentUser();
        if(audience.getId().equals(orderRequest.getAudience().getId())) {
            orderRequest.setStatus(StatusEnum.CANCEL);
        }
        return orderRequestRepository.save(orderRequest);
    }
}
