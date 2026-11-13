package com.example.myapplication

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import retrofit2.http.DELETE
import com.google.gson.annotations.SerializedName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// 로그인 요청 및 응답 데이터 클래스
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val success: Boolean, val access_token: String, val is_admin: Boolean)

// 회원가입 요청 및 응답 데이터 클래스
data class SignUpRequest(val email: String, val password: String, val username: String, val confirm_password: String, val is_admin: Boolean = false)
data class SignUpResponse(val success: Boolean, val message: String)

data class EmailRequest(val email: String)

data class VerificationResponse(val success: Boolean, val message: String, val verificationCode: String? = null)

// 비밀번호 재설정 요청 및 응답 데이터 클래스 추가
data class PasswordResetRequest(val email: String)
data class PasswordResetResponse(val success: Boolean, val message: String)

// 사용자 정보 응답 데이터 클래스
data class UserInfoResponse(
    val success: Boolean,
    val data: UserInfo
)

data class UserInfo(
    val email: String,
    val username: String
)

data class VerificationRequest(val email: String, val verification_code: String)

// 주문 내역 응답 데이터 클래스들
data class OrderHistoryResponse(
    val success: Boolean,
    val message: String,
    val data: List<OrderHistory>
)

data class OrderHistory(
    @SerializedName("order_id")
    val orderId: Int,
    @SerializedName("menu_name")
    val menuName: String,
    @SerializedName("total_price")
    val totalAmount: Int,
    @SerializedName("unit_price")
    val unitPrice: Int,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("order_time")
    val orderDate: String,
    val status: String,
    @SerializedName("completed_time")
    val completedTime: String?,
    val options: OrderOptions
)

data class OrderOptions(
    @SerializedName("hot_ice")
    val hotIceOption: String?,
    val size: String,
    val shot: Int = 0,
    val hazelnut: Int = 0,
    val vanilla: Int = 0,
    val whipping: Int = 0,
    @SerializedName("icecream")
    val iceCreamQuantity: Int = 0,
    val tapioca: Int = 0,
    val decaf: Int = 0
)

data class OrderGroup(
    val orderTime: String,
    val orders: List<OrderHistory>,
    val totalAmount: Int,
    val status: String
)

data class CartResponse(
    val success: Boolean,
    val data: List<CartItem>
)

data class CartItem(
    val menuItem: MenuItem,
    var quantity: Int,
    var hotIceOption: String = "",
    var size: String = "",
    var shotQuantity: Int = 0,
    var hazelnutQuantity: Int = 0,
    var tapiocaQuantity: Int = 0,
    var decafQuantity: Int = 0,
    var vanillaQuantity: Int = 0,
    var whippingQuantity: Int = 0,
    var iceCreamQuantity: Int = 0
)

data class FavoriteResponse(
    val success: Boolean,
    val data: List<FavoriteItem>
)

data class FavoriteItem(
    val menuName: String,
    val price: Int,
    val imageUrl: String,
    val hotIceOption: String?,
    val size: String?,
    val shotQuantity: Int = 0,
    val hazelnutQuantity: Int = 0,
    val tapiocaQuantity: Int = 0,
    val decafQuantity: Int = 0,
    val vanillaQuantity: Int = 0,
    val whippingQuantity: Int = 0,
    val iceCreamQuantity: Int = 0,
    val totalPrice: Int = 0
)

data class LogoutResponse(
    val success: Boolean,
    val message: String
)

data class MyPageResponse(
    val success: Boolean,
    val data: MyPageData
)

data class MyPageData(
    val user: UserData,
    val orders: List<OrderData>
)

data class UserData(
    val id: Int,
    val email: String,
    val username: String
)

data class OrderData(
    val id: Int,
    val total_price: Int,
    val status: String,
    val created_at: String
)

// 비밀번호 변경 요청 데이터 클래스
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

data class PasswordVerificationResponse(
    val success: Boolean,
    val message: String
)

// 이름 변경 요청 및 응답 데이터 클래스 추가
data class UpdateUsernameRequest(
    val username: String
)

data class UpdateUsernameResponse(
    val success: Boolean,
    val message: String
)

// 이메일 변경 요청 및 응답 데이터 클래스 추가
data class UpdateEmailRequest(
    val email: String
)

data class UpdateEmailResponse(
    val success: Boolean,
    val message: String
)

// 메뉴 응답 데이터 클래스
data class MenuListResponse(
    val success: Boolean,
    val data: List<MenuItemResponse>
)

data class MenuItemResponse(
    val id: Int,
    val name: String,
    val price: Int,
    val quantity: Int,
    val category: String,
    val image_url: String,
    val is_available: Boolean
)

//data class MenuItem(
//    val id: Int,
//    val name: String,
//    val price: Int,
//    val quantity: Int,
//    val category: String,
//    val image_url: String,
//    val is_available: Boolean
//)

// 장바구니 요청 데이터 클래스
data class CartOrderRequest(
    val items: List<CartOrder>
)

data class CartOrder(
    val name: String,
    val quantity: Int,
    val hotIceOption: String?,
    val size: String?,
    val shotQuantity: Int,
    val hazelnutQuantity: Int,
    val tapiocaQuantity: Int,
    val decafQuantity: Int,
    val vanillaQuantity: Int,
    val whippingQuantity: Int,
    val iceCreamQuantity: Int,
    val selectedOptions: String?
)

// 비밀번호 찾기 요청 및 응답 데이터 클래스
data class FindPasswordRequest(
    val email: String
)

data class FindPasswordResponse(
    val success: Boolean,
    val message: String
)

// Retrofit API 인터페이스
interface ApiService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>

    @POST("auth/check-email")
    fun checkEmailDuplicate(@Body request: EmailCheckRequest): Call<EmailCheckResponse>

    @POST("reset_password")
    fun resetPassword(@Body passwordResetRequest: PasswordResetRequest): Call<PasswordResetResponse>

    @GET("user/me")
    fun getUserInfo(@Header("Authorization") token: String): Call<UserInfoResponse>

    @GET("api/orders/history")
    fun getOrderHistory(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Call<OrderHistoryResponse>

    @GET("api/mypage")
    fun getMyPageInfo(@Header("Authorization") token: String): Call<MyPageResponse>

    @POST("auth/logout")
    fun logout(@Header("Authorization") token: String): Call<LogoutResponse>

    @GET("api/cart")
    fun getCartItems(@Header("Authorization") token: String): Call<CartResponse>

    @POST("/change-password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<PasswordVerificationResponse>

    @POST("/update_user")
    fun updateUsername(
        @Header("Authorization") token: String,
        @Body request: UpdateUsernameRequest
    ): Call<UpdateUsernameResponse>

    // 이메일 변경 엔드포인트 추가
    @POST("/update_email")
    fun updateEmail(
        @Header("Authorization") token: String,
        @Body request: UpdateEmailRequest
    ): Call<UpdateEmailResponse>

    // 회원가입용 인증 코드 전송
    @POST("sendVerificationCode")
    fun sendVerificationCode(@Body request: EmailRequest): Call<VerificationResponse>

    // 회원가입용 이메일 인증
    @POST("verify-email")
    fun verifyEmail(@Body request: VerificationRequest): Call<VerificationResponse>

    @GET("/api/menu")
    fun getMenus(): Call<MenuListResponse>

    @POST("api/cart/create")
    fun createCartOrder(
        @Header("Authorization") token: String,
        @Body request: CartOrderRequest
    ): Call<ApiResponse<String>>

    //    즐겨찾기
    @POST("api/favorites/update")
    fun updateFavoriteStatus(
        @Header("Authorization") token: String,
        @Body request: FavoriteUpdateRequest
    ): Call<ApiResponse<String>>

    @GET("api/favorites")
    fun getFavorites(@Header("Authorization") token: String): Call<FavoriteResponse>

    @POST("find-password")
    fun findPassword(@Body request: FindPasswordRequest): Call<FindPasswordResponse>

    // 주문 생성
    @POST("/api/orders/create")
    @Headers("Content-Type: application/json; charset=UTF-8")
    fun createOrder(
        @Header("Authorization") token: String,
        @Body orderRequest: OrderRequest
    ): Call<OrderResponse>

    // 주문 상태 확인
    @GET("api/orders/current/{orderId}")
    fun getOrderStatus(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: Int
    ): Call<OrderResponse>

    // 포스터 조회
    @GET("api/posters")
    fun getPosters(): Call<PosterResponse>

    // 결제 준비
    @Headers("Content-Type: application/json")
    @POST("/api/payments/prepare")
    fun preparePayment(
        @Header("Authorization") authorization: String,
        @Body request: PaymentPrepareRequest
    ): Call<PaymentPrepareResponse>

    @POST("api/payments/confirm")
    fun confirmPayment(
        @Header("Authorization") token: String,
        @Query("paymentKey") paymentKey: String,
        @Query("orderId") orderId: String,
        @Query("amount") amount: Int
    ): Call<PaymentConfirmResponse>

    // 주문 취소 엔드포인트 추가
    @DELETE("api/orders/{orderId}")
    fun deletePendingOrder(
        @Header("Authorization") token: String,
        @Path("orderId") orderId: Int
    ): Call<ApiResponse<String>>

}

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
)

data class FavoriteUpdateRequest(
    val menuName: String,
    val isFavorite: Boolean,
    val hotIceOption: String?,
    val size: String?,
    val shotQuantity: Int = 0,
    val hazelnutQuantity: Int = 0,
    val tapiocaQuantity: Int = 0,
    val decafQuantity: Int = 0,
    val vanillaQuantity: Int = 0,
    val whippingQuantity: Int = 0,
    val iceCreamQuantity: Int = 0,
    val totalPrice: Int,
    val price: Int,  // 기본 가격 추가
    val imageUrl: String?  // 이미지 URL 추가
)

data class MenuOptions(
    val hotIceOption: String?,
    val size: String?,
    val shotQuantity: Int = 0,
    val hazelnutQuantity: Int = 0,
    val tapiocaQuantity: Int = 0,
    val decafQuantity: Int = 0,
    val vanillaQuantity: Int = 0,
    val whippingQuantity: Int = 0,
    val iceCreamQuantity: Int = 0
)


// 주문 생성 요청 데이터 클래스
data class OrderRequest(
    val items: List<OrderItemRequest>,
)

data class OrderItemRequest(
    val menuName: String,
    val quantity: Int,
    val price: Int,  // 기본 가격
    val unitPrice: Int,  // 옵션 포함된 개별 가격
    val hotIceOption: String?,
    val size: String?,
    val shotQuantity: Int = 0,
    val hazelnutQuantity: Int = 0,
    val tapiocaQuantity: Int = 0,
    val decafQuantity: Int = 0,
    val vanillaQuantity: Int = 0,
    val whippingQuantity: Int = 0,
    val iceCreamQuantity: Int = 0
)

data class OrderResponse(
    val success: Boolean,
    val message: String,
    val data: List<OrderResponseData>
)

data class OrderResponseData(
    val order_id: Int,
    val menu_name: String,
    val total_price: Int,
    val order_time: String,
    val status: String
)

// Poster 관련 데이터 클래스 추가
data class PosterResponse(
    val success: Boolean,
    val posters: List<PosterData>
)

data class PosterData(
    val id: Int,
    val filename: String,
    val image_url: String,
    val created_at: String
)

data class PaymentPrepareRequest(
    val order_id: Int,
    val amount: Int
)

data class PaymentPrepareResponse(
    val success: Boolean,
    val paymentKey: String?,
    val orderName: String,
    val amount: Int,
    val checkoutUrl: String
)

// PaymentConfirmResponse 데이터 클래스 추가
data class PaymentConfirmResponse(
    val success: Boolean,
    val message: String,
    val payment: PaymentData? = null
)

data class PaymentData(
    val order_id: String,
    val payment_key: String,
    val amount: Int,
    val status: String,
    val payment_type: String
)