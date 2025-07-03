// Ví dụ tích hợp Activity logging vào CheckScamServiceImpl

// Thêm vào đầu class:
@Autowired
private ActivityService activityService;

// Thêm vào method checkScam() hoặc tương tự:
public ScamAnalysisResponse checkScam(CheckScamRequest request, Long userId, String userName) {
    try {
        // Logic kiểm tra scam hiện tại...
        ScamAnalysisResponse response = performScamCheck(request);
        
        // Xác định mức độ rủi ro
        String riskLevel = determineRiskLevel(response);
        
        // Log activity sau khi scan thành công
        if (userId != null && userName != null) {
            activityService.logScanActivity(
                userId, 
                userName, 
                request.getInfo(), 
                riskLevel
            );
        }
        
        return response;
        
    } catch (Exception e) {
        log.error("Error in checkScam: ", e);
        throw e;
    }
}

private String determineRiskLevel(ScamAnalysisResponse response) {
    // Logic xác định mức độ rủi ro dựa trên response
    if (response.getType() >= 3) return "high";
    if (response.getType() >= 2) return "medium";
    return "low";
}

// Tương tự cho phone check:
public PhoneCheckResponse checkPhone(String phoneNumber, Long userId, String userName) {
    PhoneCheckResponse response = performPhoneCheck(phoneNumber);
    
    // Log activity
    if (userId != null && userName != null) {
        String result = response.isScam() ? "scam" : "safe";
        activityService.logCheckActivity(userId, userName, phoneNumber, result);
    }
    
    return response;
}