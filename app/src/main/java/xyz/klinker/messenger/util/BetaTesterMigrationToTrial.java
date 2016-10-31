package xyz.klinker.messenger.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.widget.Toast;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.util.billing.BillingHelper;
import xyz.klinker.messenger.util.billing.ProductAvailableDetailed;
import xyz.klinker.messenger.util.billing.PurchasedItemCallback;

public class BetaTesterMigrationToTrial {

    private Activity context;
    private BillingHelper billing;

    public BetaTesterMigrationToTrial(Activity context) {
        this.context = context;
        this.billing = new BillingHelper(context);
    }

    public void alertToMigration() {
        new AlertDialog.Builder(context)
                .setTitle("Thanks for Testing!")
                .setMessage("The app is close to a public release, thanks to you! During this beta testing period, everything was free, but I tried to make it clear " +
                        "that the app would be subscription based after release. That starts now! Please select either the 'Start Trial' button, or the 'No Thanks' button." +
                        "\n\nThe subscription is $0.99 / month, $1.99 / three months, $5.99 / year, or a single $10.99 purchase for a lifetime account." +
                        "\n\n'No Thanks' will delete your account, and you won't be able to access the app from any devices other than your phone. But, of course, you can continue to use the app completely free on your phone, forever :)" +
                        "\n\nThe app comes with a 7 day trial. Cancelling the subscription anytime within those seven days will stop Google Play from charging your card." +
                        "Thanks for your help and continued support! If you have enjoyed the app, help me make it better by starting a subscription.")
                .setCancelable(false)
                .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(context, "Sorry :( you can always sign up again later!", Toast.LENGTH_SHORT).show();

                        Account account = Account.get(context);
                        String id = account.accountId;
                        account.clearAccount();

                        new ApiUtils().deleteAccount(id);
                    }
                })
                .setPositiveButton("Start Trial!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pickSubscription();
                    }
                }).show();
    }

    private void pickSubscription() {
        final List<ProductAvailableDetailed> available = ProductAvailableDetailed.getAllAvailableProducts(context);
        CharSequence[] titles = new CharSequence[available.size()];

        for (int i = 0; i < titles.length; i++) {
            ProductAvailableDetailed prod = available.get(i);
            titles[i] = Html.fromHtml("<b>(" + prod.getPrice() + ")</b> " + prod.getTitle() +
                    (prod.getDescription() != null ? ": " + prod.getDescription() : ""));
        }

        new android.support.v7.app.AlertDialog.Builder(context)
                .setTitle(R.string.pick_a_plan)
                .setSingleChoiceItems(titles, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        purchaseProduct(available.get(which));
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private void purchaseProduct(final ProductAvailableDetailed product) {
        if (context instanceof MessengerActivity) {
            ((MessengerActivity) context).billing = billing;
        }

        billing.purchaseItem(context, product, new PurchasedItemCallback() {
            @Override
            public void onItemPurchased(String productId) {
                Toast.makeText(context, "I am beyond happy for your support!", Toast.LENGTH_SHORT).show();
                ((MessengerActivity) context).billing = null;
                billing.destroy();
            }

            @Override
            public void onPurchaseError(final String message) {
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                        sharedPreferences.edit().putBoolean("migrate_to_trial", true).commit();

                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        ((MessengerActivity) context).billing = null;
                        billing.destroy();

                        retryNextTimeDialog();
                    }
                });
            }
        });
    }

    private void retryNextTimeDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Purchase Error or Cancellation")
                .setMessage("We will retry next time, sorry about that!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).show();
    }
}