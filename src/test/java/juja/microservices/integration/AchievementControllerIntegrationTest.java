package juja.microservices.integration;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author danil.kuznetsov
 */
@RunWith(SpringRunner.class)
public class AchievementControllerIntegrationTest extends BaseIntegrationTest {

    private static final String ACHIEVE_DAILY_URL = "/achieve/daily";
    private static final String ACHIEVE_THANKS_URL = "/achieve/thanks";
    private static final String ACHIEVE_CODENJOY_URL = "/achieve/codenjoy";
    private static final String ACHIEVE_INTERVIEW_URL = "/achieve/interview";
    private static final String ACHIEVE_WELCOME_URL = "/achieve/welcome";
    private static final String USER_POINT_SUM_URL = "/user/pointSum";
    private static final String USER_ACHIEVE_DETAILS = "/user/achieveDetails";

    private MockMvc mockMvc;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @UsingDataSet(locations = "/datasets/addNewUsersAndAchievement.json")
    public void getAllUsersWithAchievementAndReturnJson() throws Exception {
        mockMvc.perform(get(USER_POINT_SUM_URL)
                .contentType(APPLICATION_JSON_UTF8))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @UsingDataSet(locations = "/datasets/selectAchievementById.json")
    public void getUsersAchievementDetailsAndReturnJson() throws Exception {
        String json = "{\"toIds\":[\"sasha\", \"ira\"]}";
        mockMvc.perform(post(USER_ACHIEVE_DETAILS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addThanksShouldReturnValidJson() throws Exception {
        String expectedJson =
                "[{\"to\":\"ira\",\"point\":1}]";
        String jsonContentRequest =
                "{\"from\":\"sasha\",\"to\":\"ira\",\"description\":\"good work\"}";

        addAchievementsIsOk(jsonContentRequest, ACHIEVE_THANKS_URL);

        MvcResult result = getMvcResultUserAchieveSum();

        String content = result.getResponse().getContentAsString();
        assertEquals(expectedJson, content);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addThanksShouldReturnExceptionCannotThankYourself() throws Exception {
        String jsonContentRequest =
                "{\"from\":\"sasha\",\"to\":\"sasha\",\"description\":\"thanks\"}";

        addAchievementsFailed(jsonContentRequest, ACHIEVE_THANKS_URL);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addThanksShouldReturnExceptionOneThanksOnePersonInDay() throws Exception {
        String firstContentRequest =
                "{\"from\":\"sasha\",\"to\":\"ira\",\"description\":\"thanks\"}";
        String secondContentRequest =
                "{\"from\":\"sasha\",\"to\":\"ira\",\"description\":\"thanks\"}";

        addAchievementsIsOk(firstContentRequest, ACHIEVE_THANKS_URL);
        addAchievementsFailed(secondContentRequest, ACHIEVE_THANKS_URL);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addNewThanksShouldReturnExceptionNotMoreTwoThanks() throws Exception {
        String firstContentRequest =
                "{\"from\":\"sasha\",\"to\":\"ira\",\"description\":\"thanks\"}";
        String secondContentRequest =
                "{\"from\":\"sasha\",\"to\":\"max\",\"description\":\"thanks\"}";
        String thirdContentRequest =
                "{\"from\":\"sasha\",\"to\":\"peter\",\"description\":\"thanks\"}";

        addAchievementsIsOk(firstContentRequest, ACHIEVE_THANKS_URL);
        addAchievementsIsOk(secondContentRequest, ACHIEVE_THANKS_URL);
        addAchievementsFailed(thirdContentRequest, ACHIEVE_THANKS_URL);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addNewTwoThanksShouldReturnOneForMe() throws Exception {
        String expectedJson =
                "[{\"to\":\"max\",\"point\":1}," +
                        "{\"to\":\"sasha\",\"point\":1}," +
                        "{\"to\":\"ira\",\"point\":1}]";
        String firstContentRequest =
                "{\"from\":\"sasha\",\"to\":\"ira\",\"description\":\"thanks\"}";
        String secondContentRequest =
                "{\"from\":\"sasha\",\"to\":\"max\",\"description\":\"thanks\"}";

        addAchievementsIsOk(firstContentRequest, ACHIEVE_THANKS_URL);
        addAchievementsIsOk(secondContentRequest, ACHIEVE_THANKS_URL);

        MvcResult result = getMvcResultUserAchieveSum();

        String content = result.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, content, false);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addCodenjoyReturnValidJson() throws Exception {
        String jsonContentRequest =
                "{\"from\":\"max\",\"firstPlace\":\"alex\",\"secondPlace\":\"jack\",\"thirdPlace\":\"tomas\"}";
        String expectedJson =
                "[{\"to\":\"alex\",\"point\":5},{\"to\":\"jack\",\"point\":3},{\"to\":\"tomas\",\"point\":1}]";
        addAchievementsIsOk(jsonContentRequest, ACHIEVE_CODENJOY_URL);
        MvcResult result = getMvcResultUserAchieveSum();
        String content = result.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, content, false);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addCodenjoyShouldReturnExceptionFromMissed() throws Exception {
        String jsonContentRequest =
                "{\"from\":\"\",\"firstPlace\":\"alex\",\"secondPlace\":\"jack\",\"thirdPlace\":\"tomas\"}";
        addAchievementsFailed(jsonContentRequest, ACHIEVE_CODENJOY_URL);
    }

    private MvcResult getMvcResultUserAchieveSum() throws Exception {
        return mockMvc
                .perform(MockMvcRequestBuilders.get(USER_POINT_SUM_URL).contentType(APPLICATION_JSON_UTF8))
                .andReturn();
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addInterviewReturnValidJson() throws Exception {
        String jsonContentRequest = "{\"from\":\"sasha\",\"description\":\"interview report\"}";
        String expectedJson = "[{\"to\":\"sasha\",\"point\":10}]";

        mockMvc.perform(post(ACHIEVE_INTERVIEW_URL)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        MvcResult result = getMvcResultUserAchieveSum();
        String content = result.getResponse().getContentAsString();

        assertEquals(expectedJson, content);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addInterviewShouldReturnExceptionFromMissed() throws Exception {
        String jsonContentRequest = "{\"from\":\"sasha\",\"description\":\"\"}";

        mockMvc.perform(post(ACHIEVE_INTERVIEW_URL)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addDailyReturnValidJson() throws Exception {
        String jsonContentRequest = "{\"from\":\"sasha\",\"description\":\"text\"}";
        String expectedJson = "[{\"to\":\"sasha\",\"point\":1}]";

        mockMvc.perform(post(ACHIEVE_DAILY_URL)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        MvcResult result = getMvcResultUserAchieveSum();
        String content = result.getResponse().getContentAsString();

        assertEquals(expectedJson, content);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addSecondDailyReturnValidJson() throws Exception {
        String expectedJson = "[{\"to\":\"sasha\",\"point\":1}]";
        String firstContentRequest = "{\"from\":\"sasha\",\"description\":\"text\"}";
        String secondContentRequest = "{\"from\":\"sasha\",\"description\":\"add text\"}";

        addAchievementsIsOk(firstContentRequest, ACHIEVE_DAILY_URL);
        addAchievementsIsOk(secondContentRequest, ACHIEVE_DAILY_URL);

        MvcResult result = getMvcResultUserAchieveSum();

        String content = result.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, content, false);
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addDailyShouldReturnExceptionFromMissed() throws Exception {
        String jsonContentRequest = "{\"from\":\"\",\"description\":\"daily description\"}";

        mockMvc.perform(post(ACHIEVE_DAILY_URL)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addDailyShouldReturnExceptionDescriptionMissed() throws Exception {
        String jsonContentRequest = "{\"from\":\"sasha\",\"description\":\"\"}";

        mockMvc.perform(post(ACHIEVE_DAILY_URL)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    @Test
    @UsingDataSet(locations = "/datasets/initEmptyDb.json")
    public void addWelcomeShouldReturnValidJson() throws Exception {
        String expectedJson =
                "[{\"to\":\"john\",\"point\":1}]";
        String jsonContentRequest =
                "{\"from\":\"max\",\"to\":\"john\"}";

        addAchievementsIsOk(jsonContentRequest, ACHIEVE_WELCOME_URL);

        MvcResult result = getMvcResultUserAchieveSum();

        String content = result.getResponse().getContentAsString();
        assertEquals(expectedJson, content);
    }

    private void addAchievementsIsOk(String jsonContentRequest, String urlTemplate) throws Exception {
        mockMvc.perform(post(urlTemplate)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
    }

    private void addAchievementsFailed(String jsonContentRequest, String urlTemplate) throws Exception {
        mockMvc.perform(post(urlTemplate)
                .contentType(APPLICATION_JSON_UTF8)
                .content(jsonContentRequest))
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }
}