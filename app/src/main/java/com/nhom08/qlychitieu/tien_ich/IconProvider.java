package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import com.nhom08.qlychitieu.R;
import java.util.Arrays;
import java.util.List;

public class IconProvider {

    public static List<String> getExpenseIcons(Context context) {
        return Arrays.asList(
                context.getString(R.string.icon_restaurant),
                context.getString(R.string.icon_fastfood),
                context.getString(R.string.icon_local_dining),
                context.getString(R.string.icon_shopping_cart),
                context.getString(R.string.icon_local_cafe),
                context.getString(R.string.icon_coffee),
                context.getString(R.string.icon_local_drink),
                context.getString(R.string.icon_icecream),
                context.getString(R.string.icon_cake),
                context.getString(R.string.icon_local_pizza),
                context.getString(R.string.icon_kebab_dining),
                context.getString(R.string.icon_directions_car),
                context.getString(R.string.icon_car_repair),
                context.getString(R.string.icon_local_taxi),
                context.getString(R.string.icon_directions_bus),
                context.getString(R.string.icon_directions_bike),
                context.getString(R.string.icon_motorcycle),
                context.getString(R.string.icon_local_gas_station),
                context.getString(R.string.icon_health_and_safety),
                context.getString(R.string.icon_local_hospital),
                context.getString(R.string.icon_medical_services),
                context.getString(R.string.icon_school),
                context.getString(R.string.icon_local_library),
                context.getString(R.string.icon_book),
                context.getString(R.string.icon_movie),
                context.getString(R.string.icon_sports_esports),
                context.getString(R.string.icon_sports),
                context.getString(R.string.icon_sports_soccer),
                context.getString(R.string.icon_sports_volleyball),
                context.getString(R.string.icon_fitness_center),
                context.getString(R.string.icon_spa),
                context.getString(R.string.icon_casino),
                context.getString(R.string.icon_games),
                context.getString(R.string.icon_videogame_asset),
                context.getString(R.string.icon_local_grocery_store),
                context.getString(R.string.icon_local_mall),
                context.getString(R.string.icon_storefront),
                context.getString(R.string.icon_shopping_bag),
                context.getString(R.string.icon_checkroom),
                context.getString(R.string.icon_work),
                context.getString(R.string.icon_business_center),
                context.getString(R.string.icon_construction),
                context.getString(R.string.icon_handyman),
                context.getString(R.string.icon_account_balance_wallet),
                context.getString(R.string.icon_credit_card),
                context.getString(R.string.icon_savings),
                context.getString(R.string.icon_card_giftcard),
                context.getString(R.string.icon_payments),
                context.getString(R.string.icon_currency_exchange),
                context.getString(R.string.icon_trending_up),
                context.getString(R.string.icon_trending_down),
                context.getString(R.string.icon_money),
                context.getString(R.string.icon_receipt),
                context.getString(R.string.icon_request_quote),
                context.getString(R.string.icon_child_friendly),
                context.getString(R.string.icon_pets),
                context.getString(R.string.icon_phone),
                context.getString(R.string.icon_wifi),
                context.getString(R.string.icon_music_note),
                context.getString(R.string.icon_palette),
                context.getString(R.string.icon_brush),
                context.getString(R.string.icon_camera_alt),
                context.getString(R.string.icon_headphones),
                context.getString(R.string.icon_local_florist),
                context.getString(R.string.icon_toys),
                context.getString(R.string.icon_celebration),
                context.getString(R.string.icon_sports_bar),
                context.getString(R.string.icon_notifications),
                context.getString(R.string.icon_event),
                context.getString(R.string.icon_flight),
                context.getString(R.string.icon_hotel),
                context.getString(R.string.icon_home),
                context.getString(R.string.icon_house),
                context.getString(R.string.icon_confirmation_number),
                context.getString(R.string.icon_emoji_people),
                context.getString(R.string.icon_people),
                context.getString(R.string.icon_groups),
                context.getString(R.string.icon_laundry)
        );
    }

    public static List<String> getIncomeIcons(Context context) {
        return Arrays.asList(
                context.getString(R.string.icon_account_balance),
                context.getString(R.string.icon_account_balance_wallet),
                context.getString(R.string.icon_credit_card),
                context.getString(R.string.icon_savings),
                context.getString(R.string.icon_card_giftcard),
                context.getString(R.string.icon_payments),
                context.getString(R.string.icon_currency_exchange),
                context.getString(R.string.icon_trending_up),
                context.getString(R.string.icon_attach_money),
                context.getString(R.string.icon_work),
                context.getString(R.string.icon_business_center)
        );
    }
}