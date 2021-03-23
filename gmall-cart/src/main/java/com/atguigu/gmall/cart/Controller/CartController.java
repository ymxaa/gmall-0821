package com.atguigu.gmall.cart.Controller;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String saveCart(Cart cart){
        cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart.html?skuId="+cart.getSkuId();
    }

    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCarts(@PathVariable("userId")Long userId){
        List<Cart> carts = cartService.queryCheckedCarts(userId);
        return ResponseVo.ok(carts);
    }

    @GetMapping("addCart.html")
    public String toCart(@RequestParam("skuId")Long skuId, Model model){
        Cart cart = cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart",cart);

        return "addCart";
    }

    @GetMapping("cart.html")
    public String queryCart(Model model){
        List<Cart> carts =  cartService.queryCart();
        model.addAttribute("carts", carts);

        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam Long skuId){
        cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

}
