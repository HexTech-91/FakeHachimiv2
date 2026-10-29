package net.hachimi.client.util.render.animation;

public enum EasingPlus {
   Linear {
      @Override
      public double ease(double p) {
         return p;
      }
   },
   SineOut {
      @Override
      public double ease(double p) {
         return Math.sin(p * Math.PI / 2.0);
      }
   },
   SineInOut {
      @Override
      public double ease(double p) {
         return -(Math.cos(Math.PI * p) - 1.0) / 2.0;
      }
   },
   CubicIn {
      @Override
      public double ease(double p) {
         return Math.pow(p, 3.0);
      }
   },
   CubicOut {
      @Override
      public double ease(double p) {
         return 1.0 - Math.pow(1.0 - p, 3.0);
      }
   },
   CubicInOut {
      @Override
      public double ease(double p) {
         return p < 0.5 ? 4.0 * Math.pow(p, 3.0) : 1.0 - Math.pow(-2.0 * p + 2.0, 3.0) / 2.0;
      }
   },
   QuadIn {
      @Override
      public double ease(double p) {
         return Math.pow(p, 2.0);
      }
   },
   QuadOut {
      @Override
      public double ease(double p) {
         return 1.0 - (1.0 - p) * (1.0 - p);
      }
   },
   QuadInOut {
      @Override
      public double ease(double p) {
         return p < 0.5 ? 8.0 * Math.pow(p, 4.0) : 1.0 - Math.pow(-2.0 * p + 2.0, 4.0) / 2.0;
      }
   },
   QuartIn {
      @Override
      public double ease(double p) {
         return Math.pow(p, 4.0);
      }
   },
   QuartOut {
      @Override
      public double ease(double p) {
         return 1.0 - Math.pow(1.0 - p, 4.0);
      }
   },
   QuartInOut {
      @Override
      public double ease(double p) {
         return p < 0.5 ? 8.0 * Math.pow(p, 4.0) : 1.0 - Math.pow(-2.0 * p + 2.0, 4.0) / 2.0;
      }
   },
   QuintIn {
      @Override
      public double ease(double p) {
         return Math.pow(p, 5.0);
      }
   },
   QuintOut {
      @Override
      public double ease(double p) {
         return 1.0 - Math.pow(1.0 - p, 5.0);
      }
   },
   QuintInOut {
      @Override
      public double ease(double p) {
         return p < 0.5 ? 16.0 * Math.pow(p, 5.0) : 1.0 - Math.pow(-2.0 * p + 2.0, 5.0) / 2.0;
      }
   },
   CircIn {
      @Override
      public double ease(double p) {
         return 1.0 - Math.sqrt(1.0 - Math.pow(p, 2.0));
      }
   },
   CircOut {
      @Override
      public double ease(double p) {
         return Math.sqrt(1.0 - Math.pow(p - 1.0, 2.0));
      }
   },
   CircInOut {
      @Override
      public double ease(double p) {
         return p < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * p, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * p + 2.0, 2.0)) + 1.0) / 2.0;
      }
   },
   Expo {
      @Override
      public double ease(double p) {
         return p == 0.0 ? 0.0 : (p == 1.0 ? 1.0 : (p < 0.5 ? Math.pow(2.0, 20.0 * p - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * p + 10.0)) / 2.0));
      }
   },
   BackOut {
      @Override
      public double ease(double p) {
         double c1 = 1.70158;
         double c3 = c1 + 1.0;
         return 1.0 + c3 * Math.pow(p - 1.0, 3.0) + c1 * Math.pow(p - 1.0, 2.0);
      }
   },
   BackInOut {
      @Override
      public double ease(double p) {
         return p < 0.5
            ? Math.pow(2.0 * p, 2.0) * (7.189819 * p - 2.5949095) / 2.0
            : (Math.pow(2.0 * p - 2.0, 2.0) * (3.5949095 * (p * 2.0 - 2.0) + 2.5949095) + 2.0) / 2.0;
      }
   },
   Bounce {
      @Override
      public double ease(double p) {
         if (p < 0.36363636363636365) {
            return 7.5625 * p * p;
         } else if (p < 0.7272727272727273) {
            p -= 0.5454545454545454;
            return 7.5625 * p * p + 0.75;
         } else if (p < 0.9090909090909091) {
            p -= 0.8181818181818182;
            return 7.5625 * p * p + 0.9375;
         } else {
            p -= 0.9545454545454546;
            return 7.5625 * p * p + 0.984375;
         }
      }
   };

   public abstract double ease(double var1);
}
