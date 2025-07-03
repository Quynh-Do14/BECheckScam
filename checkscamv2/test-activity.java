// Test file để debug ActivityService
// Thêm vào NewsController như một endpoint debug

@PostMapping("/test-activity")
public ResponseEntity<String> testActivity() {
    try {
        System.out.println("=== Testing ActivityService directly ===");
        
        // Test 1: Basic activity creation
        System.out.println("Test 1: Creating basic activity...");
        activityService.logPostActivity(1L, "Test User", "Test News Title", "test");
        
        // Test 2: Check if activities exist
        System.out.println("Test 2: Checking activities...");
        var activities = activityService.getActivities(10, 0, null);
        System.out.println("Found " + activities.size() + " activities");
        
        return ResponseEntity.ok("Activity test completed. Check console logs.");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
}
