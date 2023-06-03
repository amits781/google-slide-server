//public class Solution {
//    public static boolean isPalindrome(int x) {
//        String rev = "";
//        String num = x+"";
//        num = num.trim();
//        while(num.length() > 0){
//            char c = num.charAt(num.length() -1);
//            rev = c + rev;
//            num = num.subString(0,num.length() -1);
//        }
//        return true;
//    }
//
//    public static void main(String args[]){
//        System.out.println(isPalindrome(42))
//    }
//}