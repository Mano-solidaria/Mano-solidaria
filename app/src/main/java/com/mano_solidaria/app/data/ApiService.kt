import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiServiceLogin {
    @Multipart
    @POST("upload")
    suspend fun subirImagen(@Part image: MultipartBody.Part): Response<Map<String, String>>
}