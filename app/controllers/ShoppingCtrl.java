package controllers;

import controllers.security.CheckIfCustomer;
import controllers.security.Secured;
import models.products.Product;
import models.shopping.Basket;
import models.shopping.OrderItem;
import models.shopping.ShopOrder;
import models.users.Customer;
import models.users.User;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;
import views.html.*;

// Import models
// Import security controllers

@Security.Authenticated(Secured.class)

@With(CheckIfCustomer.class)

public class ShoppingCtrl extends Controller {

    // Get a user - if logged in email will be set in the session
	private Customer getCurrentUser() {
		return (Customer)User.getLoggedIn(session().get("email"));
	}

    @Transactional
    public Result addToBasket(Long id) {

        // Find the product
        Product p = Product.find.byId(id);

        // Get basket for logged in customer
        Customer customer = (Customer)User.getLoggedIn(session().get("email"));

        // Check if item in basket
        if (customer.getBasket() == null) {
            // If no basket, create one
            customer.setBasket(new Basket());
            customer.getBasket().setCustomer(customer);
            customer.update();
        }
        // Add product to the basket and save
        customer.getBasket().addProduct(p);
        customer.update();

        // Show the basket contents
        return ok(basket.render(customer));
    }



    // Empty Basket
    @Transactional
    public Result emptyBasket() {
        
        Customer c = getCurrentUser();
        c.getBasket().removeAllItems();
        c.getBasket().update();
        
        return ok(basket.render(c));
    }


    
    // View an individual order
    @Transactional
    public Result viewOrder(long id) {
        ShopOrder order = ShopOrder.find.byId(id);
        return ok(orderConfirmed.render(getCurrentUser(), order));
    }

    @Transactional
    public Result showBasket() {

        return ok(basket.render(getCurrentUser()));
    }

    // Add an item to the basket
    @Transactional
    public Result addOne(Long itemId) {

        // Get the order item
        OrderItem item = OrderItem.find.byId(itemId);
        // Increment quantity
        item.increaseQty();
        // Save
        item.update();
        // Show updated basket
        return redirect(routes.ShoppingCtrl.showBasket());
    }

    @Transactional
    public Result removeOne(Long itemId) {

        // Get the order item
        OrderItem item = OrderItem.find.byId(itemId);
        // Get user
        Customer c = getCurrentUser();
        // Call basket remove item method
        c.getBasket().removeItem(item);
        c.getBasket().update();
        // back to basket
        return ok(basket.render(c));
    }

    @Transactional
    public Result placeOrder () {
        Customer c = getCurrentUser();

        //Create an order instance
        ShopOrder order = new ShopOrder();

        //Associate order with customer
        order.setCustomer(c);

        //Copy Basket to order
        order.setItems(c.getBasket().getBasketItems());

        //save the order now to generate a new id for this order
        order.save();

        //move items form basket to order
        for (OrderItem i: order.getItems()) {

            //Associate with order
            i.setOrder(order);
            //remove from basket
            i.setBasket(null);
            //update item
            i.update();
        }
        //Update order
        order.update();

        //clear and update the shopping basket
        c.getBasket().setBasketItems(null);
        c.getBasket().update();

        return ok(orderConfirmed.render(c, order));
    }
}